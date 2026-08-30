# Plan 04 — Estrategia de corte incremental

## Objetivo

Definir el orden de despliegue entre `clair-core` y `edge` para ejecutar los
planes 01-03 sin dejar ventanas de pérdida de sincronización más largas de lo
estrictamente necesario, con puntos de verificación y rollback en cada fase.

## Alcance

Solo cubre la secuencia de cambios de código/config/datos y su verificación
funcional. No cubre despliegue físico, empaquetado ni infraestructura (fuera
de alcance de esta migración, según se acordó).

## Principio de corte

El riesgo central es de **orden entre repos**: si se retira Kafka del edge
antes de que el nuevo mecanismo HTTP exista, el edge queda ciego. Si se
despliega el roster/nuevos endpoints en core antes de que el edge sepa
llamarlos, no pasa nada malo (son aditivos y no rompen lo existente) — por lo
tanto **el orden seguro es: primero core, después edge**, y dentro de cada
repo, primero lo aditivo y al final lo destructivo (borrar Kafka).

## Fases

### Fase 0 — Preparación (sin impacto en producción)

1. Confirmar mecanismo de migraciones de esquema en `clair-core`
   (`clair-core/src/main/resources/db/migration/` o similar) y escribir la
   migración de `deleted` + índice en `updated_at` sobre `devices` (plan 02).
2. Confirmar el mecanismo de `init_db` en `edge/shared/infrastructure/database.py`
   y preparar la migración local de SQLite (`deleted`, `synced_at`,
   `sync_watermark`).
3. Generar y acordar los dos secretos `EDGE_TOKEN` (ya existe) y
   `EDGE_TO_CORE_TOKEN` (nuevo) como variables de entorno en ambos repos.
4. Escribir los tests de contrato de los nuevos endpoints (plan 02 y 03) en
   `clair-core` en modo "red" (fallan porque el endpoint no existe todavía).

**Criterio de salida**: migraciones probadas en local/staging, secretos
distribuidos, tests nuevos escritos y en rojo.

### Fase 1 — `clair-core`: aditivo, sin romper nada existente

1. Aplicar la migración de `devices` (columna `deleted`, índice `updated_at`).
2. Implementar `Device.markDeleted()` y asegurar que toda mutación relevante
   actualiza `updatedAt` (plan 02).
3. Implementar el endpoint roster `GET /api/v1/edge/devices?since=`.
4. Implementar los endpoints `GET /api/v1/edge/commands/pending`,
   `POST /api/v1/edge/commands/{id}/ack`, `GET /api/v1/edge/alerts/pending`,
   `POST /api/v1/edge/alerts/{id}/ack`, `POST /api/v1/edge/presence` (plan 03).
4bis. Implementar `POST /api/v1/evaluations/telemetry/batch` (plan 03, sección
   5.1) para el camino edge→core de telemetría. El endpoint singular
   `POST /api/v1/evaluations/telemetry` (`TelemetryEvaluationController.java:57`)
   no cambia.
5. Cambiar `EdgeEventPublisher` para exponer `notifyChange(resource, hint)` y
   actualizar los tres publishers (`AlertIncidentsChangedPublisher`,
   `DeviceCommandsPendingPublisher`, `ProvisioningDevicesChangedPublisher`)
   para usarlo. **En este punto el webhook sigue apuntando a
   `/api/v1/edge/notify`, que el edge todavía no expone** — es aceptable
   porque `sendPost` ya tolera 404 sin romper el flujo de negocio (mismo
   comportamiento tolerante que hoy).
6. Limpiar restos muertos de Kafka en `clair-core` (`application.yml`,
   javadocs) — plan 01, sección clair-core. Esto es independiente y puede ir
   en el mismo PR o en uno separado, sin afectar el orden de corte.
7. Desplegar `clair-core`. **No hay impacto funcional todavía para el edge**
   actual (que sigue con sus consumers Kafka intentando conectarse a un broker
   inexistente, tal como está hoy — no empeora ni mejora su situación).

**Verificación**: smoke test manual/automatizado de
`GET /api/v1/edge/devices?since=0` con token válido devuelve el catálogo
completo; sin token, 401. Los endpoints de commands/alerts/presence responden
según contrato con datos de prueba. `POST /api/v1/evaluations/telemetry/batch`
acepta un batch de prueba y devuelve `results` correlacionados por
`client_ref`.

**Rollback**: revertir el deploy de `clair-core` a la versión anterior; no hay
cambios de esquema destructivos (solo columna nueva con default), por lo que
el rollback de aplicación es seguro incluso si la migración de BD ya corrió.

### Fase 2 — `edge`: aditivo (pollers nuevos conviven con Kafka apagándose)

1. Aplicar la migración local de SQLite.
2. Implementar `device_roster_poller.py`, el poller de comandos y el poller de
   alertas (plan 03), apuntando a los endpoints ya desplegados en Fase 1.
3. Implementar `CorePresenceHttpPublisher` y cambiar
   `DevicePresenceApplicationService` para usarlo en vez de
   `KafkaPresencePublisher`.
4. Implementar el endpoint receptor `POST /api/v1/edge/notify` que dispara los
   pollers bajo demanda.
5. Implementar `HttpCoreContextFacadeImpl` (nueva clase, implementa
   `CoreContextFacade`) apuntando a `POST /api/v1/evaluations/telemetry/batch`
   y `POST /api/v1/edge/commands/{id}/ack`, y cambiar
   `ExternalCoreService.__init__` para instanciarla en vez de
   `KafkaCoreContextFacadeImpl` (plan 01, plan 03 sección 5.1/5.2). **Este
   paso y el siguiente van en el mismo commit/PR**: no debe existir un estado
   intermedio donde `kafka_core_context_facade.py` ya esté borrado pero
   `HttpCoreContextFacadeImpl` todavía no exista, porque el
   `TelemetryOutboxProcessor` dejaría de tener transporte funcional y
   empezaría a acumular `dead_letter` (ver riesgo en `01-eliminar-kafka.md`).
6. **Retirar Kafka del edge** (plan 01, inventario completo de archivos a
   borrar): consumers, publisher de presencia, `KafkaCoreContextFacadeImpl`,
   clientes de infraestructura, imports en `edge/app.py`, dependencia en
   `pyproject.toml`/`uv.lock`. El outbox (`TelemetryOutboxProcessor`,
   `OutboxRepository`, `OutboxEntry`, tabla `device_outbox`) **no se toca en
   este paso**: sigue arrancando igual en `edge/app.py`, solo con
   `HttpCoreContextFacadeImpl` ya inyectado desde el paso 5.
7. Desplegar `edge`.

**Verificación**:
- El edge arranca y ejecuta un sync completo (`since=0`) contra core,
  poblando su caché local de devices.
- Crear/modificar/borrar un device en core y confirmar que el edge lo refleja
  dentro del intervalo de poll configurado (sin depender de la notificación).
- Enviar un comando de prueba desde core y confirmar que el edge lo recoge vía
  poll y hace ack correctamente.
- Provocar un incidente de alerting de prueba y confirmar el mismo flujo.
- Confirmar que la transición de presencia detectada por telemetría en el
  edge llega a core vía `POST /api/v1/edge/presence`.
- Generar telemetría de prueba en el edge y confirmar que
  `TelemetryOutboxProcessor` la envía exitosamente vía
  `HttpCoreContextFacadeImpl` al endpoint batch, que la entrada de outbox
  queda `sent`, y que el registro aparece consultable en
  `GET /api/v1/evaluations/devices/{deviceId}` de core. Confirmar también que
  un fallo simulado de red produce `retry` con backoff y, tras agotar
  reintentos, `dead_letter` (no debe perderse silenciosamente).
- Confirmar el ack de un comando ejecutado por un dispositivo embebido llega a
  core vía `POST /api/v1/edge/commands/{id}/ack` desde
  `HttpCoreContextFacadeImpl.publish_command_acknowledged`.
- `grep -rin kafka edge` (excluyendo `.venv`) no devuelve resultados.

**Rollback**: si el edge nuevo falla en producción, revertir al binario
anterior. Como el core de Fase 1 sigue siendo compatible hacia atrás (el
webhook viejo `EdgeEventPublisher` ya fue reemplazado por `notifyChange`, que
tolera 404), el edge anterior (con Kafka) seguiría "funcionando" tan mal como
antes del corte (sin sincronización real, porque su Kafka ya apunta a un
broker inexistente) — es decir, el rollback de edge no empeora el estado
respecto a como está hoy en `develop`. Si se requiere una vía de rollback sin
esta degradación preexistente, debe congelarse un tag de `edge` inmediatamente
antes de la Fase 2 con Kafka aún funcional contra un broker real, pero dado
que hoy no hay broker en ningún ambiente, esto es principalmente teórico.
**Caso particular del outbox de telemetría**: si tras el despliegue
`HttpCoreContextFacadeImpl` falla sistemáticamente (p. ej. error de contrato
con el endpoint batch) pero el resto del edge funciona, el rollback puede
acotarse a revertir solo el paso 5 (volver a `KafkaCoreContextFacadeImpl`)
sin revertir los pollers de Fase 2, ya que son independientes entre sí; en ese
caso la telemetría vuelve a acumularse en `dead_letter` como hoy, pero el
resto de la sincronización (devices, comandos, alertas, presencia) sigue
funcionando.

### Fase 3 — Cierre y limpieza

1. Confirmar en ambos repos que no queda referencia a Kafka (criterios de
   aceptación del plan 01).
2. Remover variables de entorno obsoletas (`KAFKA_BOOTSTRAP_SERVERS` y
   equivalentes) de los `.env`/plantillas de configuración de ambos repos.
3. Actualizar la documentación de contratos (`docs/plans/03-contratos-http-core-edge.md`
   pasa a ser la referencia viva; considerar promoverla a un doc de
   integración permanente fuera de `docs/plans/` una vez estabilizado, aunque
   esa decisión de organización de docs queda fuera de esta migración).
4. Ejecutar `05-actualizacion-documentacion.md`: reescribir los diagramas de
   clases e historias técnicas de ambos repos que todavía describen consumers
   y publishers Kafka, para que la documentación de referencia no quede
   describiendo una arquitectura que ya no existe.

## Qué probar en cada fase (resumen)

| Fase | Qué probar |
|---|---|
| 0 | Migraciones aplican y son reversibles en un entorno de prueba |
| 1 | Endpoints nuevos en core responden según contrato; suite de tests de `clair-core/src/test` pasa completa, incluyendo los tests adaptados de los tres publishers |
| 2 | Edge sincroniza correctamente en frío y en caliente; ack de comandos/alertas idempotente; ausencia total de Kafka |
| 3 | Limpieza de config; ninguna referencia residual a Kafka en ningún repo; diagramas y historias técnicas actualizados (plan 05) |

## Feature flags / variables de entorno relevantes

No se introduce un feature flag de aplicación (el cambio es estructural, no
opcional), pero sí variables de entorno nuevas que actúan como puntos de
control operacional:

- `EDGE_TO_CORE_TOKEN` (nuevo, ambos repos) — habilita/deshabilita
  efectivamente el acceso del edge a los endpoints de core si se rota o se
  deja sin configurar (fail closed).
- `EDGE_POLL_INTERVAL_SECONDS` (nuevo, edge) — permite ajustar agresividad del
  polling en caliente sin redeploy si se expone como variable de entorno leída
  en cada ciclo (a decidir en implementación si se relee dinámicamente o solo
  al arranque).
- `edge.webhook-url` / `edge.token` (existentes en `application.yml` de core,
  vía `@Value`) — se mantienen, ahora usados por `notifyChange`.

## Riesgos globales del corte

- **Riesgo de ventana intermedia**: entre el fin de la Fase 1 y el fin de la
  Fase 2, el edge sigue corriendo su versión vieja con Kafka roto — es decir,
  sin sincronización real durante ese lapso, igual que está hoy. No es una
  regresión, pero tampoco se soluciona hasta que Fase 2 termine; se recomienda
  minimizar el tiempo entre ambas fases (mismo día si es posible).
- **Riesgo de doble escritura durante pruebas**: si se prueba el poller nuevo
  en un entorno que comparte la misma base Postgres/SQLite de producción, un
  test mal aislado podría contaminar datos reales. *Mitigación*: usar un
  entorno de staging con su propia base de datos para las verificaciones de
  Fase 1 y 2.
- **Riesgo no cubierto por este plan**: la ingesta de telemetría sin batch
  (`TelemetryEvaluationController.evaluateTelemetry`) no cambia con esta
  migración; si el volumen de dispositivos crece, seguirá siendo un cuello de
  botella independiente de todo lo aquí descrito.

## Criterio de aceptación global del corte

1. Todas las fases completadas y verificadas en orden.
2. `clair-core` y `edge` en `develop` sin ninguna referencia a Kafka.
3. Sincronización de devices, comandos, alertas y presencia funcionando
   end-to-end vía HTTP, verificado manualmente apagando y reencendiendo cada
   lado por separado.
4. Suite de tests existente de `clair-core/src/test` en verde, incluyendo los
   tests nuevos/adaptados de los planes 01-03.
