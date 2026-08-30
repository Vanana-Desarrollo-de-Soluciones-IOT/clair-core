# Plan 02 — Sincronización de devices: pull idempotente reconciliador

## Objetivo

Reemplazar el push fire-and-forget actual (roto y sin reintento) por un
mecanismo donde la corrección del catálogo de devices en el edge esté
garantizada por un **pull idempotente con watermark**, usando el push
únicamente como optimización de latencia.

## Alcance

- Cambios de modelo en `Device` (clair-core): marca temporal de actualización,
  versión monotónica, soft delete con tombstone.
- Endpoint roster en clair-core: `GET /api/v1/edge/devices?since=<watermark>`.
- Poller en el edge con watermark persistido en SQLite.
- Degradación del webhook push existente a mera notificación.
- No incluye comandos ni alerting (eso es el plan 03, que reutiliza el mismo
  patrón de poller pero para otros recursos).

## Impacto en el dominio (DDD)

### Bounded context `device` (clair-core) — Aggregate `Device`

`Device` (`clair-core/src/main/java/com/claircore/device/domain/model/entities/Device.java`)
es hoy una entidad JPA plana sin versión de dominio explícita ni soft delete;
solo tiene `DeviceAudit auditFields` (`Device.java:41`, extiende
`AuditableModel`, probablemente con `createdAt`/`updatedAt` genéricos de
Spring Data Auditing).

Cambios necesarios:

1. **Confirmar y, si falta, añadir `updatedAt` en `AuditableModel`**
   (`clair-core/src/main/java/com/claircore/shared/domain/model/entities/AuditableModel.java`)
   con `@LastModifiedDate` gestionado por `AuditingEntityListener` (ya presente
   en `Device.java:12`). Este campo pasa a ser el **watermark de sincronización**:
   toda mutación de `Device` (creación, rotación de api key, cambio de nombre,
   cambio de estado, borrado lógico) debe tocarlo.
2. **Soft delete con tombstone**: añadir a `Device`
   - `@Column(name = "deleted", nullable = false) private boolean deleted = false;`
   - Un método de dominio `markDeleted()` que solo cambia el flag (no borra la
     fila), de modo que el roster pueda seguir devolviendo el registro con
     `deleted=true` para que el edge lo purgue de su caché.
   - El repositorio (`DeviceRepository`, capa de persistencia) deja de hacer
     `DELETE` físico sobre `devices` cuando la baja proviene de un flujo que
     deba propagarse al edge; se sustituye por `markDeleted()` + `save()`. Si
     hay un caso de uso de borrado físico legítimo (limpieza administrativa),
     se mantiene aparte y no participa del roster.
3. **Estado (`status`)**: no se toca su semántica de dominio existente; el
   roster simplemente expone el valor actual (`ONLINE`/`OFFLINE`/lo que ya
   maneje `Device`), igual que hoy lo hace `DeviceChangedIntegrationEvent`.

No se introduce un nuevo agregado ni un bounded context nuevo: el cambio vive
enteramente dentro de `device` y es aditivo sobre el agregado existente.
`ProvisioningDevicesChangedPublisher` (`clair-core/src/main/java/com/claircore/device/application/internal/outboundservices/acl/ProvisioningDevicesChangedPublisher.java`)
se conserva pero cambia su propósito (ver "Webhook degradado" más abajo).

### Bounded context `provisioning` (edge)

`DeviceProvisioningApplicationService`
(`edge/provisioning/application/services/device_provisioning_application_service.py`)
y `DeviceCacheRepository`
(`edge/provisioning/infrastructure/device_cache_repository.py`) ya son
idempotentes por `device_id` vía `upsert_many` con `on_conflict` (líneas 16-31
de `device_cache_repository.py`). Se reutilizan sin cambios de contrato interno;
se añade:

- Un campo `deleted` (booleano) y `updated_at` (o `synced_at`) en el modelo
  Peewee `DeviceModel` (`edge/iam/infrastructure/models.py` o donde esté
  definido — verificar ubicación real antes de migrar) para poder marcar
  tombstones localmente y saltarse registros ya sincronizados.
- Una tabla/fila de metadatos de sincronización (`sync_state` o similar) que
  guarde el último `watermark` recibido del roster, para no tener que traer
  todo el catálogo en cada poll.

## Quality-attribute scenarios (ADD)

| Escenario | Requisito |
|---|---|
| **Disponibilidad — core caído** | El edge debe seguir operando con el último catálogo cacheado; el poller reintenta con backoff y no bloquea el resto del servicio Flask (hilo daemon aislado, igual que hacían los consumers Kafka). |
| **Disponibilidad — edge caído** | Los cambios de `Device` ocurridos en core mientras el edge está caído deben ser recuperables al reiniciar el edge, mediante un pull completo (`since=0`) o incremental (`since=<último watermark>`) — nunca se pierden porque no dependen de una ventana de entrega en tiempo real. |
| **Consistencia — reconciliación eventual** | Tras un pull con éxito, el estado del edge para todo device con `updated_at <= now_roster_query` debe converger exactamente al estado de core (incluyendo bajas). Esto se verifica con watermark monotónico: no se "pierde" ningún cambio entre dos polls consecutivos siempre que `since` sea el máximo `updated_at` visto, inclusive. |
| **Seguridad — roster autenticado** | El roster expone `api_key` de cada device (dato sensible). El endpoint debe requerir `EDGE_TO_CORE_TOKEN` y viajar sobre TLS; no debe ser alcanzable sin autenticación bajo ninguna circunstancia (fail closed: 401 si falta o es inválido el token, nunca "devolver vacío"). |
| **Rendimiento — polling no debe saturar core** | El intervalo de poll y el tamaño de página del roster deben mantenerse razonables (p. ej. poll cada 30-60s, página de hasta 200 registros) dado que el catálogo de devices es pequeño comparado con telemetría; no se diseña para miles de rps. |

## Contrato del endpoint roster (resumen; detalle completo en `03-contratos-http-core-edge.md`)

```
GET /api/v1/edge/devices?since=<epoch_millis_o_iso8601>&limit=<n>
Headers: X-Edge-Token: <EDGE_TO_CORE_TOKEN>

200 OK
{
  "watermark": "2026-08-28T10:15:30.123Z",
  "devices": [
    {
      "device_id": "uuid",
      "hardware_id": "string",
      "api_key": "string",
      "status": "ONLINE|OFFLINE|...",
      "deleted": false,
      "updated_at": "2026-08-28T10:14:00.000Z"
    }
  ],
  "has_more": false
}
```

- `since` es exclusivo: el servidor devuelve devices con `updated_at > since`.
  Se recomienda usar el propio `updated_at` del último registro recibido (o el
  `watermark` de la respuesta) como `since` de la siguiente llamada — nunca el
  reloj local del edge, para evitar desfases de reloj entre máquinas.
- `watermark` en la respuesta es el `now()` del servidor de core al momento de
  ejecutar la consulta (o el `max(updated_at)` de la página si se prefiere
  evitar drift); se documenta cuál de las dos semánticas se implementa
  finalmente en el plan 03 junto con el resto de endpoints, para no duplicar
  la decisión.
- `has_more`/paginación: si el catálogo excede `limit`, el edge repite la
  llamada con el `since` del último elemento de la página hasta agotar el
  backlog, antes de persistir el nuevo watermark global.

## Poller del edge y su watermark

Módulo nuevo: `edge/provisioning/application/device_roster_poller.py` (sustituye
en responsabilidad a `kafka_provisioning_consumer.py`, eliminado en el plan 01).

1. Al arrancar (`edge/app.py`, dentro de `initialize()`): dispara un ciclo de
   sync completo leyendo el watermark persistido (0 si es la primera vez) y
   pagina hasta `has_more=false`.
2. Hilo daemon en loop con `sleep(POLL_INTERVAL_SECONDS)` (env var, default
   sugerido 30s) que repite el mismo ciclo incremental.
3. Cada registro recibido se pasa por
   `DeviceProvisioningApplicationService` (extendido, ver plan 01) que
   reutiliza `DeviceCacheService.validate_device_record` +
   `DeviceCacheRepository.upsert_many`; si `deleted=true`, se marca el registro
   local como tombstone (no se borra físicamente, para no romper claves foráneas
   de telemetría/alerting local si las hubiera) o se borra físicamente si el
   esquema del edge no tiene tal dependencia — a decidir en implementación
   según el esquema real de `iam/infrastructure/models.py`.
4. El watermark solo avanza **después** de persistir con éxito todos los
   registros de la página (transacción o al menos orden estricto: primero
   upsert, luego avanzar watermark) para que un fallo a mitad de página se
   reintente desde el mismo punto y no pierda registros.

## Webhook degradado a notificación

`EdgeEventPublisher.publishDeviceChanged` (`clair-core/src/main/java/com/claircore/shared/infrastructure/edge/EdgeEventPublisher.java:38-40`)
deja de enviar el payload completo del device. En su lugar:

- Nueva ruta única de notificación: `POST /api/v1/edge/notify` con body mínimo
  `{"resource": "device", "device_id": "uuid"}` (o incluso sin body, solo un
  ping).
- El edge, al recibir la notificación, no confía en el contenido del payload:
  dispara inmediatamente un ciclo del poller (`device_roster_poller.trigger()`),
  que hace el pull real y autoritativo.
- Si la notificación no llega (edge caído, red caída, 404, etc.), el poll
  periódico igual la recoge en el siguiente ciclo — de ahí que el `catch`
  actual en `EdgeEventPublisher.java:52-55` que solo loguea sea aceptable en
  este nuevo diseño (antes era un bug porque no había red de seguridad; ahora
  el poll es esa red de seguridad).
- `ProvisioningDevicesChangedPublisher.publish` (`ProvisioningDevicesChangedPublisher.java:19-22`)
  se actualiza para invocar `edgeEventPublisher.notifyChange("device", event.deviceId())`
  en lugar de `publishDeviceChanged(event)` con el payload completo.

## Idempotencia

- **Del lado de core**: la actualización de `updatedAt` es monotónica por fila
  gracias a `@LastModifiedDate`; no se requiere lógica adicional de
  deduplicación en la escritura.
- **Del lado del edge**: `upsert_many` con `on_conflict` sobre `device_id`
  (`device_cache_repository.py:23-31`) ya es idempotente ante reprocesar el
  mismo registro varias veces (reintentos del poller, notificaciones
  duplicadas, arranque + poll superpuestos).
- **Ante relojes desincronizados**: como se indicó, `since` se basa en el
  `updated_at` devuelto por el propio servidor, no en el reloj del cliente,
  evitando que un edge con reloj adelantado/atrasado pierda registros.

## Comportamiento ante caídas

| Escenario | Comportamiento esperado |
|---|---|
| Core caído durante el poll | El poller captura la excepción HTTP, loguea, no actualiza el watermark, reintenta en el siguiente ciclo. El edge sigue sirviendo con el catálogo cacheado. |
| Edge caído cuando core notifica | La notificación POST falla o no se entrega; al reiniciar el edge, el sync completo de arranque (`since=<último watermark persistido>`) recupera cualquier cambio perdido. |
| Ambos caídos simultáneamente y se recuperan en cualquier orden | No hay pérdida: el watermark persiste en SQLite en el edge y en la columna `updated_at` en Postgres en core; ninguno de los dos lados depende de estado en memoria. |
| Borrado de un device en core mientras el edge está caído | Al reconectar, el pull trae el registro con `deleted=true` y `updated_at` posterior al watermark local, y el edge aplica el tombstone. Sin este campo, la baja nunca se propagaría (limitación actual sin este plan). |

## Migraciones de esquema

- **Postgres (clair-core)**: migración (Flyway/Liquibase, según lo que use el
  proyecto — confirmar en `clair-core/src/main/resources/db/migration/` antes
  de implementar) que añade `deleted boolean not null default false` a
  `devices`, y confirma/crea índice sobre `updated_at` para que el filtro
  `since` del roster no haga table scan (`CREATE INDEX idx_devices_updated_at
  ON devices(updated_at);`).
- **SQLite (edge)**: migración local (el mecanismo que use `shared/infrastructure/database.py`
  para `init_db`) que añade `deleted` y `synced_at`/`updated_at` a la tabla de
  devices, y una tabla `sync_watermark(resource TEXT PRIMARY KEY, value TEXT)`
  para persistir el watermark por recurso (reutilizable para commands/alerts
  en el plan 03).

## Tests requeridos

- **clair-core**
  - `DeviceTest` (dominio): `markDeleted()` cambia `deleted=true` y no elimina
    el registro; `updatedAt` se actualiza en cada mutación relevante (nombre,
    api key, estado).
  - Test de repositorio/integración: `GET /api/v1/edge/devices?since=X` devuelve
    solo registros con `updated_at > X`; devuelve `401` sin token o con token
    inválido; pagina correctamente cuando el catálogo excede `limit`.
  - Test de `ProvisioningDevicesChangedPublisher`: verifica que ahora invoca la
    notificación liviana y no serializa el payload completo del device (ajustar
    `ProvisioningDevicesChangedPublisherTest.java`).
- **edge**
  - Test de `device_roster_poller`: dado un roster con dos páginas, aplica
    ambas y solo avanza el watermark al final de la segunda; ante fallo en la
    segunda página, el siguiente poll reintenta desde el watermark de la
    primera.
  - Test de idempotencia: aplicar la misma página del roster dos veces no
    duplica registros y no falla.
  - Test de tombstone: un registro con `deleted=true` se refleja localmente
    (soft delete o borrado, según lo decidido) y el device deja de autenticar
    vía `AuthApplicationService.authenticate` (`iam/application/services.py`)
    si corresponde a la regla de negocio actual de "no autenticar devices
    dados de baja".

## Riesgos y tradeoffs

- **Riesgo**: si `updatedAt` no se toca consistentemente en todas las
  mutaciones de `Device` (p. ej. un `save()` directo que salte el listener de
  auditoría), el roster podría omitir cambios. *Mitigación*: centralizar toda
  mutación a través de métodos de dominio (`rotateApiKey`, `updateName`,
  `markDeleted`, cambio de estado) y no exponer setters públicos sueltos —
  ya es el estilo actual del agregado (`Device.java:57-70`).
- **Tradeoff descartado — outbox transaccional del lado de `clair-core` para
  este flujo (core→edge)**: se evaluó introducir en `clair-core` una tabla de
  eventos pendientes con un relay que los publique hacia el edge. Se descartó
  *para este flujo* porque duplica lo que ya resuelve el pull con watermark
  (garantía de entrega eventual) a cambio de más piezas (tabla outbox, proceso
  relay, limpieza de eventos antiguos) sin ganancia real, dado que el volumen
  de cambios de `Device` es bajo. **Esta decisión es específica de la
  dirección core→edge y no aplica al outbox que ya existe en el edge para la
  dirección edge→core** (telemetría y acks de comandos, ver
  `01-eliminar-kafka.md`): ese outbox se conserva y se reapunta a HTTP, porque
  ahí el problema es distinto (alto volumen, el edge es el productor del dato,
  y no existe un "pull" simétrico con sentido — core no puede "tirar" de
  telemetría que aún no se generó).
- **Riesgo de reloj/orden**: si dos devices se actualizan con el mismo
  `updated_at` (resolución de milisegundos, alta concurrencia teórica), el
  filtro `since > X` podría, en un caso límite, omitir uno si ambos comparten
  exactamente el mismo timestamp y uno ya fue consumido antes de que el otro
  se confirmara. *Mitigación*: usar timestamp con mayor precisión
  (`Instant`/`timestamptz` con microsegundos) y, si se requiere garantía
  estricta, añadir un desempate por `id` en el `ORDER BY` del roster.

## Criterios de aceptación

1. `Device.java` expone `markDeleted()` y `updatedAt` se refleja en la
   respuesta del roster.
2. `GET /api/v1/edge/devices?since=0` devuelve el catálogo completo autenticado;
   sin token devuelve 401.
3. Apagar el core, crear/modificar/borrar devices no es posible (esperado);
   apagar el edge, hacer cambios en core, y al reencender el edge, verificar
   que el catálogo converge sin intervención manual.
4. Ejecutar el mismo ciclo de poll dos veces seguidas con el mismo `since` no
   produce cambios adicionales ni duplicados (idempotencia verificada).
