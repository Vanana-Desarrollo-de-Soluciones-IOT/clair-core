# Plan 05 — Actualización de la documentación existente

## Objetivo

Alinear los diagramas de clases y las historias técnicas de ambos repos con la
arquitectura real resultante de los planes 01-04 (HTTP directo, sin broker),
incluyendo la deuda preexistente de `clair-core`: sus diagramas ya describían
clases Kafka (`*KafkaConsumer`, `*KafkaPublisher`) que dejaron de existir en el
código desde `45d405f remove: kafka`, mucho antes de esta migración. Ningún
lector nuevo de estos documentos debe poder concluir que el sistema usa un
broker de mensajes.

## Alcance

- Reescritura de diagramas de clases (`docs/class-diagrams/`) e historias
  técnicas (`docs/user-stories/`) en ambos repos que mencionan Kafka.
- Revisión (no necesariamente reescritura) de los `context-mapping/context-mapping.md`
  de ambos repos.
- No incluye crear documentación nueva de arquitectura fuera de lo ya
  cubierto por los planes 01-04; este plan solo corrige lo existente para que
  no contradiga la migración.

## Posición en el orden de ejecución

Este plan se ejecuta **al final**, en la Fase 3 de `04-plan-de-corte.md`
("Cierre y limpieza"), después de que el código de los planes 01-03 esté
desplegado y estable. Reescribir los diagramas antes correría el riesgo de
documentar una interfaz que todavía puede cambiar durante la implementación.
La excepción es la parte de `clair-core` que documenta Kafka *intra-monolito*
(consumers/publishers entre bounded contexts de evaluation/alerting/analytics/
notifications): esa deuda es independiente de esta migración y puede
corregirse en paralelo, en cualquier momento, sin esperar a los planes 01-04.

## Inventario — `edge/docs/`

Estos archivos describen el mecanismo Kafka edge-interno (consumers de
`device`, `alerting`, `provisioning`, publisher de `iam`) que el plan 01
retira. Se reescriben para reflejar pollers/facades HTTP.

| Archivo | Menciones de Kafka | Qué pasa a describir |
|---|---|---|
| `edge/docs/class-diagrams/iam_class_diagram.md` (11 menciones: `KafkaPresencePublisher`, `IamKafkaTopics`, atributo `kafka_client`, relaciones `DevicePresenceApplicationService --> KafkaPresencePublisher`, `KafkaPresencePublisher --> IamKafkaTopics`) | Clase y relaciones de publicación de presencia | Reemplazar `KafkaPresencePublisher` por `CorePresenceHttpPublisher` (plan 03, sección 5) con atributo `-http_client` o similar en vez de `-kafka_client`; eliminar `IamKafkaTopics` del diagrama (no hay topics); la relación pasa a `DevicePresenceApplicationService --> CorePresenceHttpPublisher : uses`. |
| `edge/docs/class-diagrams/alerting_class_diagram.md` (7 menciones: `KafkaAlertIncidentConsumer`, `AlertingKafkaTopics`, relaciones `--> AlertIncidentEventApplicationService`, `--> AlertingKafkaTopics`) | Consumer de incidentes desde Kafka | Reemplazar `KafkaAlertIncidentConsumer` por el poller HTTP (`AlertIncidentPoller` o nombre equivalente al que se use en implementación, plan 03 sección 4); eliminar `AlertingKafkaTopics`; la relación pasa a `AlertIncidentPoller --> AlertIncidentEventApplicationService : uses` y se añade `AlertIncidentPoller --> CoreAlertingHttpClient : uses` (o el nombre que reciba el cliente HTTP hacia `GET /api/v1/edge/alerts/pending` / `POST /api/v1/edge/alerts/{id}/ack`). |
| `edge/docs/class-diagrams/provisioning_class_diagram.md` (7 menciones: `KafkaProvisioningConsumer`, `ProvisioningKafkaTopics`, relaciones asociadas) | Consumer de aprovisionamiento desde Kafka | Reemplazar `KafkaProvisioningConsumer` por `DeviceRosterPoller` (plan 02); eliminar `ProvisioningKafkaTopics`; añadir la relación con el watermark persistido (`SyncWatermarkRepository` o equivalente) y con el cliente HTTP hacia `GET /api/v1/edge/devices`. |
| `edge/docs/user-stories/technical_stories_unified.md` (2 menciones, ES-US-02 y ES-US-03, idénticas a las de los archivos por contexto) | Escenarios Gherkin que mencionan "tema de Kafka" y "consumidor de Kafka" | Reescribir ES-US-02 ("Publicación de cambio de presencia en Kafka") como "Publicación de cambio de presencia vía HTTP a clair-core", cambiando "publica un evento de integración... en el tema de Kafka `clair.iam.devices.presence`" por "envía un `POST /api/v1/edge/presence` a clair-core con los campos device_id, hardware_id, status y occurred_at". Reescribir ES-US-03 ("Sincronizar Evento de Aprovisionamiento...") para describir el poller de roster (`GET /api/v1/edge/devices?since=`) en vez de "consumir eventos desde un tema de Kafka"; los tres escenarios internos (procesar payload válido, rechazar payload incompleto, soportar camelCase/snake_case) se conservan tal cual porque describen la lógica de normalización de `DeviceProvisioningApplicationService`, que no cambia — solo cambia el origen del mensaje. |
| `edge/docs/user-stories/iam-technical-stories.md` (1 mención, mismo texto que ES-US-02 arriba) | Mismo escenario de presencia vía Kafka | Mismo tratamiento que en `technical_stories_unified.md`. |
| `edge/docs/user-stories/provisioning-technical-stories.md` (1 mención, mismo texto que ES-US-03 arriba) | Mismo escenario de aprovisionamiento vía Kafka | Mismo tratamiento que en `technical_stories_unified.md`. |

Nota: ninguno de estos archivos documenta hoy el camino edge→core de
telemetría vía outbox (`TelemetryOutboxProcessor`) ni el ack de comandos —
revisar si existe una historia técnica separada para telemetría/outbox no
detectada en el grep de "kafka" (podría mencionar el transporte de forma
indirecta, p. ej. "Kafka" en minúscula sin coincidir con el patrón, o sin
mencionar el transporte en absoluto). Si existe, debe actualizarse igual para
reflejar `HttpCoreContextFacadeImpl` en vez de `KafkaCoreContextFacadeImpl`,
aunque el outbox en sí (comportamiento observable) no cambia.

## Inventario — `clair-core/docs/`

Estos archivos documentan Kafka **intra-monolito** (comunicación entre
bounded contexts dentro del mismo proceso Spring), que ya no existe desde
`45d405f`. El código real usa `ApplicationEventPublisher` +
`@EventListener`/oyentes ACL nombrados `*EventListener`, no consumers. Esta es
deuda documental preexistente, no introducida por esta migración, pero se
corrige en el mismo esfuerzo por estar directamente relacionada.

| Archivo | Menciones de Kafka | Estado real en el código | Qué pasa a describir |
|---|---|---|---|
| `clair-core/docs/class-diagrams/device-bc.md` (12 menciones: `ProvisioningDevicesChangedKafkaPublisher`, `DeviceCommandsPendingKafkaPublisher`, atributo `-KafkaTemplate kafkaTemplate` en ambas) | `ProvisioningDevicesChangedPublisher` y `DeviceCommandsPendingPublisher` (clases reales, `clair-core/src/main/java/com/claircore/device/application/internal/outboundservices/acl/`) ya no usan `KafkaTemplate`: dependen de `EdgeEventPublisher` (HTTP, hoy roto; plan 02/03 lo arregla y lo renombra a `notifyChange`). | Renombrar las clases del diagrama quitando el sufijo "Kafka" (`ProvisioningDevicesChangedPublisher`, `DeviceCommandsPendingPublisher`), reemplazar el atributo `-KafkaTemplate kafkaTemplate` por `-EdgeEventPublisher edgeEventPublisher`, y actualizar el método público a `publish(...)` invocando `edgeEventPublisher.notifyChange(resource, hint)` (post plan 03). |
| `clair-core/docs/class-diagrams/alerting-bc.md` (11 menciones: `AlertIncidentsChangedKafkaPublisher`, `AlertingTelemetryRecordedKafkaConsumer`, atributos `-KafkaTemplate kafkaTemplate`, relaciones `AlertingTelemetryRecordedKafkaConsumer --> AlertCommandServiceImpl`, `AlertCommandServiceImpl --> AlertIncidentsChangedKafkaPublisher`) | El consumo de telemetría dentro de `alerting` es un listener de evento Spring: `AlertingTelemetryRecordedEventListener` (`clair-core/src/main/java/com/claircore/alerting/application/internal/inboundservices/acl/AlertingTelemetryRecordedEventListener.java`), no un Kafka consumer. La publicación de incidentes usa `AlertIncidentsChangedPublisher` sobre `EdgeEventPublisher` (HTTP). | Renombrar `AlertingTelemetryRecordedKafkaConsumer` → `AlertingTelemetryRecordedEventListener`, quitar el atributo de infraestructura Kafka y reflejar que escucha `TelemetryRecordedEvent` vía `@EventListener`/`ApplicationEventPublisher`. Renombrar `AlertIncidentsChangedKafkaPublisher` → `AlertIncidentsChangedPublisher`, reemplazar `-KafkaTemplate kafkaTemplate` por `-EdgeEventPublisher edgeEventPublisher` y `-ApplicationEventPublisher eventPublisher` (el publisher real usa ambos, ver `AlertIncidentsChangedPublisher.java:15-16`). |
| `clair-core/docs/class-diagrams/evaluation-bc.md` (8 menciones: `TelemetryRecordedKafkaConsumer`, atributo `-KafkaInboxService kafkaInboxService`) | No existe tal consumer en el código: `TelemetryEvaluationController.evaluateTelemetry` (`TelemetryEvaluationController.java:57`) procesa la telemetría de forma síncrona dentro de la misma petición HTTP; no hay un inbox ni un consumer separado para `evaluation`. | Eliminar por completo la clase `TelemetryRecordedKafkaConsumer` y `KafkaInboxService` del diagrama de `evaluation`: no tienen equivalente real. Documentar en su lugar que `TelemetryEvaluationCommandServiceImpl.handle` se invoca directamente desde el controller REST. Si en algún momento existió un patrón inbox real y se quiere conservar la idea de desacoplar el guardado de la validación, debe verificarse en el código actual antes de inventar una clase nueva — no crear documentación de algo que no existe. |
| `clair-core/docs/class-diagrams/notifications-bc.md` (8 menciones: `AlertIncidentChangedKafkaConsumer`, relaciones con `PushNotificationDeliveryService`, `PushNotificationHistoryRepository`, `ExternalDeviceService`, `ExternalAlertingService`) | La clase real es `AlertIncidentChangedEventListener` (`clair-core/src/main/java/com/claircore/notifications/application/internal/inboundservices/acl/AlertIncidentChangedEventListener.java`), oyente de `AlertIncidentChangedEvent` vía Spring events. | Renombrar `AlertIncidentChangedKafkaConsumer` → `AlertIncidentChangedEventListener`, mismas relaciones salientes (se mantienen si el código real las conserva — verificar durante la reescritura, no asumir que son idénticas). |
| `clair-core/docs/class-diagrams/analytics-bc.md` (4 menciones: `TelemetryAnalyticKafkaConsumer`, relación `--> KpiLiveMetricsCommandServiceImpl`) | Verificar la clase real equivalente en `com.claircore.analytics.application.internal.inboundservices.acl` (probablemente `AnalyticsTelemetryRecordedEventListener` o nombre análogo al de `alerting`, a confirmar leyendo el paquete antes de escribir el diagrama). | Renombrar según la clase real encontrada, quitando toda referencia a Kafka. |

## Revisión de `context-mapping.md` en ambos repos

Se revisaron ambos documentos como parte de este plan (no solo se asumió):

- **`clair-core/docs/context-mapping/context-mapping.md`**: describe
  relaciones `U -> D [ACL]` entre bounded contexts (Device, Alerting,
  Analytics, Evaluation, Notifications, Billing, IAM) y `SK` (shared kernel)
  hacia `Shared`. **No menciona transporte** (ni "Kafka" ni "HTTP" ni
  "evento"): el diagrama es agnóstico del mecanismo de comunicación, solo
  expresa la dirección de dependencia (upstream/downstream) y el patrón de
  integración (ACL). **Conclusión: no requiere cambios.** El hecho de que el
  transporte interno pase de Kafka a eventos Spring no altera qué contexto es
  upstream/downstream de cuál.
- **`edge/docs/context-mapping/context-mapping.md`**: describe relaciones
  `U -> D [ACL]` entre Device, Alerting, Provisioning e IAM, y `SK` hacia
  `Shared`, **dentro del propio edge** (no representa la relación con
  `clair-core` en absoluto — no aparece un nodo "Core" ni "clair-core" en el
  diagrama). Es decir, este documento nunca modeló la integración
  edge↔core, por lo que la migración de esa integración a HTTP no lo afecta.
  **Conclusión: no requiere cambios**, aunque se señala como una brecha
  preexistente (no introducida por esta migración) que el context mapping del
  edge no representa su dependencia externa hacia `clair-core` — se deja
  anotado como posible mejora futura, fuera de alcance de esta migración.

## Cómo ejecutar la reescritura (pasos)

1. Para cada archivo de la tabla de `edge/docs/`, releer la clase real
   correspondiente en el código ya migrado (post plan 01-03) antes de escribir
   el diagrama, para no documentar nombres de clase supuestos que difieran de
   la implementación final (los nombres de clase citados en este plan, p. ej.
   `DeviceRosterPoller`, `CorePresenceHttpPublisher`, son sugerencias
   consistentes con los planes 01-03, pero la implementación real es la
   fuente de verdad).
2. Para cada archivo de la tabla de `clair-core/docs/`, confirmar el nombre
   exacto de cada `*EventListener`/`*Publisher` real antes de renombrar en el
   diagrama (varios ya se confirmaron en este plan; `analytics-bc.md` queda
   pendiente de verificación puntual del nombre de clase).
3. Actualizar las historias técnicas del edge reescribiendo únicamente los
   fragmentos Gherkin que mencionan Kafka, preservando el resto del escenario
   (dado/cuando/entonces) que describe comportamiento de negocio invariante.
4. No tocar los `context-mapping.md` de ningún repo (ver justificación
   arriba), salvo que una revisión futura decida modelar explícitamente la
   dependencia edge→core, lo cual queda fuera de esta migración.

## Riesgos y tradeoffs

- **Riesgo**: renombrar clases en los diagramas sin verificar el nombre real
  final introduce una nueva desincronización documentación-código, el mismo
  problema que se está corrigiendo. *Mitigación*: este plan se ejecuta después
  de que el código esté desplegado y estable (Fase 3 de `04-plan-de-corte.md`),
  nunca antes ni en paralelo a la implementación.
- **Riesgo**: la deuda de `clair-core` (Kafka intra-monolito) es anterior a
  esta migración y podría percibirse como fuera de alcance. *Decisión*: se
  incluye explícitamente porque el coordinador lo pidió y porque dejarla sin
  corregir perpetuaría documentación activamente engañosa sobre una
  arquitectura que ya no existe desde antes de este esfuerzo.
- **Tradeoff**: se optó por no crear un documento de arquitectura nuevo que
  centralice el estado "post-migración" de ambos repos, y en su lugar corregir
  los documentos existentes in-place, para no duplicar fuentes de verdad.

## Tests requeridos

No aplica (documentación, no código). Como criterio de calidad equivalente a
"test": cada diagrama y cada historia técnica reescrita debe poder
verificarse línea por línea contra una clase o endpoint real existente en el
repositorio correspondiente en el momento de la reescritura.

## Criterios de aceptación

1. `grep -rli kafka edge/docs` no devuelve resultados.
2. `grep -rli kafka clair-core/docs/class-diagrams` no devuelve resultados
   (fuera de estos planes de migración, que sí documentan Kafka
   intencionalmente como estado pasado).
3. Cada clase nombrada en los diagramas de clases actualizados existe con ese
   nombre exacto en el código fuente correspondiente.
4. Los `context-mapping.md` de ambos repos permanecen sin cambios, con la
   justificación de por qué documentada en este plan.
