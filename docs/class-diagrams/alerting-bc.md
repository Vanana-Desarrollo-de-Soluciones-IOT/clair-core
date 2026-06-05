# Alerting Bounded Context Class Diagram

This diagram displays the 4-layer DDD architecture for the Alerting Bounded Context.

```mermaid
---
title: DDD Class Diagram - Alerting Bounded Context (4 Layers)
---

classDiagram

namespace interfaces {
    class AlertController {
        -AlertQueryService alertQueryService
        +getAlertsByDevice(deviceId, page, size) ResponseEntity
        +getAlertsBySpace(spaceId, page, size) ResponseEntity
        +getDailySummary(spaceId) ResponseEntity
    }
    class AlertingContextFacade {
        <<interface>>
        +findAlertDetailsById(alertId) Optional~AlertDetails~
    }
}

namespace application {
    class AlertCommandServiceImpl {
        -AlertRepository alertRepository
        -ExternalAlertingThresholdService externalThresholdService
        -ExternalAlertingDeviceService externalDeviceService
        -AlertIncidentsChangedKafkaPublisher alertIncidentsChangedKafkaPublisher
        +handle(EvaluateTelemetryForAlertsCommand) void
    }
    class AlertQueryServiceImpl {
        -AlertRepository alertRepository
        -ExternalAlertingDeviceService externalDeviceService
        +handle(GetAlertsByDeviceQuery) Page~Alert~
        +handle(GetAlertsBySpaceQuery) Page~Alert~
        +handle(GetAlertsByOwnerQuery) List~DailyAlertCount~
    }
    class AlertingContextFacadeImpl {
        -AlertRepository alertRepository
        +findAlertDetailsById(alertId) Optional~AlertDetails~
    }
    class AlertingTelemetryRecordedKafkaConsumer {
        -AlertCommandService alertCommandService
        -ObjectMapper objectMapper
        +consume(record) void
    }
    class ExternalAlertingDeviceService {
        <<interface>>
        +fetchSpaceIdByDeviceId(deviceId) Optional~UUID~
        +fetchSpaceNameBySpaceId(spaceId) Optional~String~
        +fetchDeviceNameByDeviceId(deviceId) Optional~String~
        +fetchHardwareIdByDeviceId(deviceId) Optional~String~
    }
    class ExternalAlertingThresholdService {
        <<interface>>
        +fetchEnabledThresholdsByDeviceId(deviceId) List~DeviceMetricThresholdConfiguration~
    }
    class EvaluateTelemetryForAlertsCommand {
        +UUID deviceId
        +BigDecimal co2
        +BigDecimal pm25
        +BigDecimal temperature
        +BigDecimal humidity
        +Instant occurredAt
    }
}

namespace domain {
    class Alert {
        -UUID id
        -UUID deviceId
        -UUID spaceId
        -MetricType metric
        -BigDecimal thresholdValue
        -BigDecimal actualValue
        -String message
        -AlertStatus status
        -AlertSeverity severity
        -String spaceName
        -String deviceName
        -Instant occurredAt
        -Instant resolvedAt
        +acknowledge() void
        +resolve(resolvedAt) void
    }
    class MetricType {
        <<enumeration>>
        CO2
        PM25
        TEMPERATURE
        HUMIDITY
    }
    class AlertSeverity {
        <<enumeration>>
        LOW
        WARNING
        CRITICAL
    }
    class AlertStatus {
        <<enumeration>>
        ACTIVE
        ACKNOWLEDGED
        RESOLVED
    }
    class DailyAlertCount {
        +LocalDate date
        +Long count
    }
    class AlertRepository {
        <<interface>>
        +save(alert) Alert
        +findById(id) Optional~Alert~
        +findByDeviceIdAndMetricAndStatusIn(deviceId, metric, statuses) Optional~Alert~
        +findBySpaceIdAndStatus(spaceId, status, pageable) Page~Alert~
        +findFirstByDeviceIdAndMetricAndStatusIn(deviceId, metric, statuses) Optional~Alert~
    }
}

namespace infrastructure {
    class JpaAlertRepository {
        <<interface>>
        +findFirstByDeviceIdAndMetricAndStatusIn(deviceId, metric, statuses)
    }
    class AlertIncidentsChangedKafkaPublisher {
        -KafkaTemplate kafkaTemplate
        +publish(AlertIncidentChangedIntegrationEvent)
    }
}

AlertController --> AlertQueryServiceImpl : uses
AlertingTelemetryRecordedKafkaConsumer --> AlertCommandServiceImpl : uses

AlertCommandServiceImpl --> Alert : creates/updates
AlertCommandServiceImpl --> AlertRepository : uses
AlertCommandServiceImpl --> ExternalAlertingDeviceService : uses
AlertCommandServiceImpl --> ExternalAlertingThresholdService : uses
AlertCommandServiceImpl --> AlertIncidentsChangedKafkaPublisher : publishes

AlertQueryServiceImpl --> AlertRepository : uses

AlertingContextFacadeImpl ..|> AlertingContextFacade : implements
AlertingContextFacadeImpl --> AlertRepository : uses

Alert --> MetricType : contains
Alert --> AlertSeverity : contains
Alert --> AlertStatus : contains

JpaAlertRepository ..|> AlertRepository : implements
```
