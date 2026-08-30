# Plan 01 — Eliminar Kafka de ambos repos

## Objetivo

Erradicar toda dependencia de Kafka en `edge` (donde sigue viva) y limpiar los
restos muertos que quedaron en `clair-core` tras `45d405f remove: kafka`, sin
perder ninguna capacidad funcional que hoy dependa de los topics: comandos de
dispositivo, incidentes de alerting, aprovisionamiento de devices y presencia de
IAM.

## Alcance

- Retirar código, dependencias y configuración de Kafka.
- **No** implementa todavía el mecanismo de reemplazo definitivo (eso es
  `02-sincronizacion-devices.md` y `03-contratos-http-core-edge.md`); aquí solo
  se detiene el uso de Kafka y se sustituye por el HTTP que ya existía de forma
  parcial (polling clásico), documentando explícitamente qué queda temporalmente
  degradado hasta que 02/03 se desplieguen.
- Se recomienda ejecutar 01 y 02/03 como una sola ventana de despliegue (ver
  `04-plan-de-corte.md`) para no dejar el sistema sin sincronización entre medio.

## Inventario exhaustivo — `edge`

### Camino edge→core de telemetría y acks: se CONSERVA, se reapunta a HTTP

Este camino usa el patrón outbox (escritura transaccional + worker con
reintento/backoff/circuit breaker) y **no se borra**: es exactamente la
maquinaria de entrega confiable que el edge necesita para sobrevivir cortes de
red hacia core, ya construida y probada. Lo único que cambia es el transporte
final (Kafka → HTTP), aislado detrás de la interfaz `CoreContextFacade`.

| Archivo | Qué hace hoy | Qué cambia |
|---|---|---|
| `edge/device/application/outbox_processor.py` (`TelemetryOutboxProcessor`) | Worker en background (`start()` en `edge/app.py:99`) que hace polling del outbox (`BATCH_SIZE = 10`, `outbox_processor.py:40`), aplica backoff exponencial (`_calculate_next_retry`, líneas 173-186) y circuit breaker (`CircuitBreaker`, líneas 46-48, 93-97) antes de invocar `ExternalCoreService.publish_telemetry_recorded`. | **Se conserva sin cambios de lógica.** Solo cambian los comentarios/docstring que mencionan "Kafka" (líneas 1-4, 29-32) por "HTTP". |
| `edge/device/application/outboundservices/acl/external_core_service.py` (`ExternalCoreService`) | ACL que delega en `self._facade`, hoy instanciado como `KafkaCoreContextFacadeImpl()` (línea 24). | **Se conserva.** Cambia únicamente la implementación por defecto inyectada: pasa a ser `HttpCoreContextFacadeImpl()`. Actualizar el docstring del módulo (líneas 1-6, "via Kafka" → "via HTTP"). |
| `edge/device/application/outboundservices/acl/core_context_facade.py` (`CoreContextFacade`, interfaz ABC) | Define el contrato `publish_telemetry_recorded` / `publish_command_acknowledged`. | **Se conserva la interfaz sin cambios de firma** — es el punto exacto que permite sustituir el transporte sin tocar el resto de la aplicación. Actualizar el docstring (menciona "via Kafka" en líneas 1-5, 12). |
| `edge/device/application/outboundservices/acl/kafka_core_context_facade.py` (`KafkaCoreContextFacadeImpl`) | Implementación Kafka de la interfaz: publica a `DeviceKafkaTopics.TELEMETRY_RECORDED` y `DeviceKafkaTopics.COMMANDS_ACKNOWLEDGED` (líneas 34, 51). | **Se borra** y se reemplaza por una nueva clase `HttpCoreContextFacadeImpl` en el mismo paquete (`http_core_context_facade.py`), que implementa los mismos dos métodos posteando a `clair-core` (contrato en `03-contratos-http-core-edge.md`). |
| `edge/device/domain/outbox_entry.py` (`OutboxEntry`) | Entidad de dominio del outbox: `aggregate_type`, `aggregate_id`, `event_type`, `status` (`pending`/`sent`/`dead_letter`), `retry_count`, `next_retry_at`. | **Se conserva íntegramente**, no depende de Kafka en absoluto. |
| `edge/device/infrastructure/outbox/outbox_repository.py` (`OutboxRepository`) | Persistencia Peewee del outbox: `find_pending`, `mark_sent`, `mark_retry`, `mark_dead_letter`, `delete_sent_older_than`. | **Se conserva íntegramente**, no depende de Kafka. |
| `edge/device/infrastructure/outbox/outbox_record_model.py` | Modelo Peewee de la tabla `device_outbox`. | **Se conserva íntegramente.** |
| `edge/device/application/services.py:78-85` (`DeviceTelemetryAppService.create_full_telemetry_record`) | Escribe la entrada de outbox en la misma transacción (`db.atomic()`) que la telemetría. | **Se conserva íntegramente.** Solo se actualiza el docstring del módulo (líneas 1-6) y de la clase (líneas 34-38), que dicen "via the outbox pattern and Kafka" / "forward to clair-core via Kafka" — pasan a decir "via HTTP". |
| `edge/device/application/services.py:170` (`DeviceCommandApplicationService.acknowledge_embedded_command`, vía `self.external_core_service.publish_command_acknowledged(payload)`) | Publica el ack de un comando ejecutado/fallado por el dispositivo embebido. | **Se conserva la llamada**; el cambio de transporte es transparente porque pasa por `ExternalCoreService` → `CoreContextFacade`. |
| `edge/shared/infrastructure/database.py` (tabla `device_outbox`) | Inicialización local de la tabla de outbox; la compatibilidad de snapshots se resuelve con un modelo auxiliar Peewee creado por `init_db`. | **Se conserva la inicialización**, sin asumir una migración `_migrate_outbox_schema`. |
| `edge/device/infrastructure/reliability/circuit_breaker.py` (`CircuitBreaker`, `CircuitBreakerOpenException`) | Protección genérica de sobrecarga, agnóstica de transporte. | **Se conserva íntegramente.** |

Con esto, el único cambio real en este camino es de una línea de
composición (`ExternalCoreService.__init__`) más una clase nueva
(`HttpCoreContextFacadeImpl`) que reemplaza a `KafkaCoreContextFacadeImpl`.
Ningún otro componente del outbox se toca.

### Archivos a borrar por completo

Nota: `edge/device/application/outboundservices/acl/kafka_core_context_facade.py` también se borra, pero está documentado en la sección anterior ("Camino edge→core de telemetría y acks") junto con su reemplazo, no en esta tabla, para no separar el borrado de su sustituto directo.

| Archivo | Motivo |
|---|---|
| `edge/shared/infrastructure/kafka_client.py` | Cliente de infraestructura Kafka (`KafkaInfrastructureClient`), sin otros usos. |
| `edge/shared/infrastructure/kafka_topics.py` | Definición de `KafkaConsumerGroups` y utilidades de topics compartidas. |
| `edge/device/application/kafka_command_consumer.py` | Consumer de `clair.device.commands.pending`. Reemplazo: poller HTTP contra `GET /api/v1/edge/commands/pending` (ver `03-contratos-http-core-edge.md`). |
| `edge/device/infrastructure/kafka/device_kafka_topics.py` | Definición de topics de comandos. |
| `edge/alerting/application/kafka_alert_incident_consumer.py` | Consumer de `clair.device.alert.incident.changed`. Reemplazo: poller HTTP contra `GET /api/v1/edge/alerts/pending`. |
| `edge/alerting/infrastructure/kafka/alerting_kafka_topics.py` | Definición de topics de alerting. |
| `edge/alerting/infrastructure/kafka/` (directorio completo si queda vacío) | — |
| `edge/provisioning/application/kafka_provisioning_consumer.py` | Consumer de `clair.provisioning.devices.changed`. Reemplazo: poller de roster (`GET /api/v1/edge/devices?since=`), ver `02-sincronizacion-devices.md`. |
| `edge/provisioning/infrastructure/kafka/provisioning_kafka_topics.py` | Definición de topics de provisioning. |
| `edge/provisioning/infrastructure/kafka/` (directorio completo) | — |
| `edge/iam/application/outboundservices/acl/kafka_presence_publisher.py` | Publisher de `DevicePresenceChanged` a `clair.device.presence.changed`. Reemplazo: `POST /api/v1/edge/presence` (push best-effort, sin garantía de entrega — ver más abajo por qué eso es aceptable). |
| `edge/iam/infrastructure/kafka/iam_kafka_topics.py` | Definición de topics de IAM. |
| `edge/iam/infrastructure/kafka/` (directorio completo) | — |
| `edge/**/__pycache__/*kafka*.pyc` | Bytecode cacheado, se limpia solo al borrar los `.py` fuente y re-generar `__pycache__`; no requiere paso manual si el `.gitignore` ya los excluye (confirmar). |

### `edge/app.py` — cambios puntuales

- `edge/app.py:15` `from device.application.kafka_command_consumer import KafkaCommandConsumer` → eliminar; sustituir por el nuevo poller (`03-contratos-http-core-edge.md` define el módulo, p. ej. `device/application/command_poller.py`).
- `edge/app.py:17` `from device.infrastructure.kafka.device_kafka_topics import DeviceKafkaTopics` → eliminar.
- `edge/app.py:19-20` imports de `AlertingKafkaTopics` y `KafkaAlertIncidentConsumer` → eliminar; sustituir por el poller de alerting.
- `edge/app.py:23-24` imports de `IamKafkaTopics` → eliminar.
- `edge/app.py:25-26` imports de `KafkaProvisioningConsumer`, `ProvisioningKafkaTopics` → eliminar; sustituir por el poller de roster (ver plan 02).
- `edge/app.py:32` `from shared.infrastructure.kafka_client import KafkaInfrastructureClient` → eliminar.
- `edge/app.py:52-59` función `_collect_all_topics()` → eliminar por completo (ya no hay topics que bootstrapear).
- `edge/app.py:53` (docstring del módulo, líneas 1-4) → reescribir: quitar "starts Kafka topic bootstrapping, and launches background consumers" y describir el poller.
- `edge/app.py:56-58` instancias globales `_command_consumer`, `_provisioning_consumer`, `_alert_incident_consumer` → renombrar/sustituir por instancias del poller nuevo (`_device_roster_poller`, `_command_poller`, `_alert_poller`).
- `edge/app.py:88-99` bloque `initialize()`: eliminar el `try/except` de `kafka_client.bootstrap_topics(...)` (líneas 92-96) y reemplazar `_command_consumer.start()`, `_provisioning_consumer.start()`, `_alert_incident_consumer.start()` por el arranque de los pollers HTTP equivalentes.

### `edge/iam/application/services.py`

- `edge/iam/application/services.py:9` `from iam.application.outboundservices.acl.kafka_presence_publisher import KafkaPresencePublisher` → eliminar.
- `edge/iam/application/services.py:47-48` `self.kafka_presence_publisher = KafkaPresencePublisher()` en `DevicePresenceApplicationService.__init__` → sustituir por un nuevo ACL HTTP, p. ej. `CorePresenceHttpPublisher`, inyectado igual.
- El resto del flujo (`mark_seen`, `mark_stale_devices_offline`, `_publish_presence`) se conserva sin cambios de lógica de negocio; solo cambia el mecanismo de transporte del payload (`device_id`, `hardware_id`, `status`, `occurred_at`) — ver contrato en `03-contratos-http-core-edge.md`.
- Nota de comportamiento: hoy, si Kafka falla, `_publish_presence` solo loguea un warning (`services.py` línea siguiente a 80, `logger.warning(...)`) y no revierte el `last_seen_at` ya actualizado en SQLite. Con HTTP directo se conserva esa misma semántica "best-effort, no bloqueante": la presencia real de verdad la valida el pull de roster periódico si hace falta, no este push.

### `edge/provisioning/application/services/device_provisioning_application_service.py`

- El método público `handle_device_changed_event` deja de ser invocado por un consumer Kafka; en el plan 02 se añade un método nuevo (`sync_from_roster` o similar) que reutiliza `DeviceCacheService.validate_device_record` y `DeviceCacheRepository.upsert_many` (`device_provisioning_application_service.py:20,33`) para procesar la respuesta paginada del roster HTTP en lugar de un único payload de evento. No se borra este archivo: se extiende.
- Actualizar el docstring del módulo (líneas 1-5) que dice "Coordinates device cache updates driven entirely by Kafka events from clair-core" — ya no es cierto.

### `edge/pyproject.toml` / `edge/uv.lock`

- `edge/pyproject.toml`: eliminar la línea `"kafka-python>=2.3.1",` de `dependencies`.
- Ejecutar `uv lock` para regenerar `edge/uv.lock` sin la entrada `kafka-python` y sus transitivas (revisar que no queden huérfanas como `six` si no las usa nadie más).
- Verificar `edge/.venv` no queda referenciado en el repo (ya es un directorio de entorno virtual, no versionado — confirmar `.gitignore`).

## Inventario exhaustivo — `clair-core` (restos muertos)

| Archivo:línea | Contenido a retirar |
|---|---|
| `clair-core/src/main/resources/application.yml:31-32` | Bloque `kafka: bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}` bajo `spring:` — eliminar por completo, ya no hay `spring-kafka` en el classpath. |
| `clair-core/src/main/resources/application.yml:39` (bloque `kafkaPublisher` bajo `resilience4j.circuitbreaker.instances`, líneas 93-99 aprox.) | Sin bean `CircuitBreaker` llamado `kafkaPublisher` que lo consuma tras el retiro de Kafka; eliminar la instancia completa. |
| `clair-core/src/main/resources/application.yml` (bloque `kafkaPublisher` bajo `resilience4j.retry.instances`, líneas 109-111 aprox.) | Misma razón; eliminar. |
| `clair-core/src/main/java/com/claircore/device/application/internal/commandservices/DeviceCommandServiceImpl.java:89` | Comentario `// Edge provisioning cache is fed only via Kafka integration events.` — actualizar para reflejar el nuevo flujo HTTP (roster pull), o eliminarlo si el código de esa línea ya no aplica tras el plan 02. |
| `clair-core/src/main/java/com/claircore/evaluation/application/internal/inboundservices/acl/TelemetryRecordedIntegrationEvent.java` | Javadoc que menciona el topic Kafka de este evento — reescribir para describir el evento interno de Spring (`ApplicationEventPublisher`), que es el mecanismo real que ya usa. |
| `clair-core/src/main/java/com/claircore/analytics/application/internal/inboundservices/acl/TelemetryRecordedIntegrationEvent.java` | Ídem. |
| `clair-core/src/main/java/com/claircore/alerting/application/internal/inboundservices/acl/TelemetryRecordedIntegrationEvent.java` | Ídem. |
| `clair-core/src/main/java/com/claircore/device/application/internal/outboundservices/acl/DeviceChangedIntegrationEvent.java` | Javadoc con mención de topic Kafka — actualizar para describir el nuevo transporte (roster HTTP + webhook de notificación, plan 02/03). |
| `clair-core/src/main/java/com/claircore/device/application/internal/outboundservices/acl/DeviceCommandIssuedIntegrationEvent.java` | Ídem, describir `GET /api/v1/edge/commands/pending`. |
| `clair-core/src/main/java/com/claircore/device/application/internal/inboundservices/acl/DevicePresenceChangedIntegrationEvent.java` | Ídem, describir `POST /api/v1/edge/presence`. |
| `src/main/java/com/claircore/device/interfaces/rest/controllers/EdgeCommandController.java` (`POST /api/v1/edge/commands/{commandId}/ack`) + `src/main/java/com/claircore/device/application/internal/commandservices/EdgeCommandAcknowledgementService.java` | Servicio HTTP actual que recibe el ACK del edge, valida `hardware_id` y transiciona el comando: `200` si se aplica, `409` si ya es terminal y `404` si no existe, no pertenece al dispositivo o no está en estado `SENT`. El cuerpo es `{ "hardware_id": "...", "result": "OK|FAILED", "detail": "..." }`. |

Nota: en `clair-core` no queda ninguna dependencia real de Kafka en `pom.xml` /
`build.gradle` (confirmado: `grep -rn kafka` solo devuelve el `.yml` y los
javadocs listados arriba). No hay clases `KafkaTemplate`, `@KafkaListener` ni
similares — el retiro de código productivo ya ocurrió en `45d405f`; este plan
solo cierra los cabos sueltos de configuración y documentación.

## Qué funcionalidad hay que reponer para no perder capacidad

| Capacidad Kafka perdida | Reemplazo HTTP | Dónde se especifica |
|---|---|---|
| `KafkaCoreContextFacadeImpl` (edge→core) entrega telemetría con garantía de al-menos-una-vez vía el outbox | `HttpCoreContextFacadeImpl`, misma interfaz `CoreContextFacade`, postea a `POST /api/v1/evaluations/telemetry` (o a un endpoint batch nuevo, ver `03-contratos-http-core-edge.md`); el outbox, backoff y circuit breaker se conservan sin cambios | `03-contratos-http-core-edge.md` |
| `KafkaCoreContextFacadeImpl.publish_command_acknowledged` (edge→core) informa ejecución/fallo de un comando | `HttpCoreContextFacadeImpl.publish_command_acknowledged`, postea al endpoint de ack de comandos de core | `03-contratos-http-core-edge.md` |
| `KafkaCommandConsumer` (edge) entrega comandos pendientes en tiempo casi real | Poller HTTP del edge contra `GET /api/v1/edge/commands/pending`, con intervalo corto (p. ej. 5s) | `03-contratos-http-core-edge.md` |
| `KafkaAlertIncidentConsumer` (edge) entrega incidentes de alerting | Poller HTTP contra `GET /api/v1/edge/alerts/pending` | `03-contratos-http-core-edge.md` |
| `KafkaProvisioningConsumer` (edge) sincroniza el catálogo de devices | Poller de roster idempotente `GET /api/v1/edge/devices?since=` | `02-sincronizacion-devices.md` |
| `KafkaPresencePublisher` (edge→core) informa transiciones de presencia detectadas por telemetría | `POST /api/v1/edge/presence`, best-effort, no bloqueante | `03-contratos-http-core-edge.md` |
| Bootstrap de topics al arranque (`_collect_all_topics`) | No aplica: no hay topics que crear; el poller simplemente empieza a pedir con `since=0` | `02-sincronizacion-devices.md` |

## Riesgos y tradeoffs

- **Riesgo**: si se borra Kafka en `edge` antes de tener el poller (plan 02/03)
  desplegado, el edge queda sin ningún mecanismo de sincronización durante la
  ventana intermedia. *Mitigación*: coordinar 01+02+03 en el mismo corte (ver
  `04-plan-de-corte.md`); no mergear 01 solo a `develop` de `edge` en producción
  sin 02/03 listos.
- **Tradeoff descartado**: mantener Kafka solo para presencia/comandos y migrar
  el resto — se descartó porque introduce un sistema híbrido (dos mecanismos de
  transporte) que aumenta la complejidad operativa sin beneficio: el objetivo
  explícito es "sin broker".
- **Riesgo menor**: `uv.lock` con dependencias transitivas de `kafka-python`
  (p. ej. compresión `lz4`, `zstandard` si estuvieran instaladas) puede dejar
  paquetes huérfanos si no se corre `uv lock` limpio. *Mitigación*: regenerar el
  lockfile completo, no editarlo a mano.
- **Riesgo específico del outbox**: si se borra `kafka_core_context_facade.py`
  sin haber implementado y probado `HttpCoreContextFacadeImpl` primero, el
  `TelemetryOutboxProcessor` queda sin transporte funcional y toda la
  telemetría pendiente terminará en `dead_letter` tras agotar
  `MAX_RETRIES` (`outbox_processor.py:35`). *Mitigación*: el borrado de
  `kafka_core_context_facade.py` y el alta de `http_core_context_facade.py`
  deben ir en el mismo commit/PR, nunca en pasos separados; ver la secuencia
  exacta en `04-plan-de-corte.md`.
- **Tradeoff explícito, no descartado**: se decidió **conservar** el outbox
  existente en vez de reemplazarlo por un patrón distinto (p. ej. delegar toda
  la confiabilidad al pull reconciliador de `02-sincronizacion-devices.md`).
  Se descartó esa alternativa porque la telemetría es alta en volumen y de
  dirección edge→core — un pull de core hacia el edge no tiene sentido para
  este flujo (el edge es quien produce el dato); el outbox con push
  reintentado es el patrón correcto para esta dirección y ya está construido
  y probado, así que no se re-diseña sin necesidad.

## Tests requeridos

- **edge** (no hay suite de tests de Kafka detectada en el repo — confirmar con
  `find edge -iname "*test*"` antes de borrar; si aparecen tests que importan
  los módulos eliminados, deben borrarse o reescribirse contra los pollers
  nuevos en el mismo commit).
  - Test específico del outbox: con `HttpCoreContextFacadeImpl` sustituyendo a
    `KafkaCoreContextFacadeImpl`, `TelemetryOutboxProcessor._send_entry` debe
    seguir marcando `sent` en éxito, `retry` con backoff en fallo transitorio,
    y `dead_letter` tras `MAX_RETRIES` — el comportamiento de
    `outbox_processor.py` no cambia, solo el doble/mock de
    `ExternalCoreService` usado en el test.
  - Test de `ExternalCoreService`: verificar que instancia
    `HttpCoreContextFacadeImpl` por defecto y que sigue aceptando inyección de
    un facade distinto para pruebas (el constructor con `facade: CoreContextFacade
    | None = None` en `external_core_service.py:23` ya soporta esto sin
    cambios).
- **clair-core**:
  - Test de contrato/config: verificar que `application.yml` cargue sin
    `spring.kafka.*` y que el contexto Spring levante sin errores de propiedad
    faltante (smoke test de `ApplicationContext`, ya cubierto por los tests de
    integración existentes bajo `clair-core/src/test/java/com/claircore/...`).
  - Verificar que ningún `CircuitBreakerRegistry`/`RetryRegistry` intente
    resolver la instancia `kafkaPublisher` tras eliminarla de `application.yml`
    (buscar usos de `@CircuitBreaker(name = "kafkaPublisher")` — no se
    encontraron en el código actual, así que el borrado es seguro).
  - `ProvisioningDevicesChangedPublisherTest.java`,
    `DeviceCommandsPendingPublisherTest.java`: revisar que no dependan de
    supuestos sobre Kafka (actualmente dependen de `EdgeEventPublisher`, que se
    mantiene y se actualiza en el plan 03, no en este).

## Criterios de aceptación

1. `grep -rin kafka edge --include=*.py` (excluyendo `.venv`) no devuelve
   resultados.
2. `grep -n kafka edge/pyproject.toml edge/uv.lock` no devuelve resultados.
3. `grep -in kafka clair-core/src/main/resources/application.yml` no devuelve
   resultados.
4. `grep -rln "Kafka\|kafka" clair-core/src/main/java` no devuelve resultados.
5. El edge arranca (`python app.py` / suite local) sin excepciones en el hilo de
   arranque relacionadas con Kafka.
6. El contexto Spring de `clair-core` levanta limpio (`./gradlew test` o
   equivalente del proyecto) sin fallos de propiedad faltante.
