# Analytics Bounded Context Class Diagrams

This document contains both the unified Bounded Context class diagram and the layered individual diagrams.

## Unified Class Diagram

```mermaid
---
title: DDD Class Diagram - Analytics Bounded Context (4 Layers)
---

classDiagram

namespace interfaces {
    class AnalyticsController {
        -KpiDashboardMetricsQueryService kpiDashboardMetricsQueryService
        -KpiHistoricalTrendQueryService kpiHistoricalTrendQueryService
        -AnalyticsSseService analyticsSseService
        +getLiveMetrics(deviceId) ResponseEntity
        +streamLiveMetrics(deviceId) SseEmitter
        +getHistoricalMetrics(deviceId, period, startDate, endDate) ResponseEntity
        +getTrends(deviceId, period, startDate, endDate, limit) ResponseEntity
    }
    class AnalyticsOverviewController {
        -OverviewDashboardQueryService overviewDashboardQueryService
        +getOverviewDashboard() ResponseEntity
    }
    class AnalyticsContextFacade {
        <<interface>>
    }
}

namespace application {
    class KpiLiveMetricsCommandServiceImpl {
        -KpiLiveMetricsCache kpiLiveMetricsCache
        -AnalyticsSseService analyticsSseService
        +handle(ProcessTelemetryAnalyticCommand) void
    }
    class KpiDashboardMetricsQueryServiceImpl {
        -KpiLiveMetricsCache kpiLiveMetricsCache
        -DeviceAnalyticsSnapshotRepository deviceAnalyticsSnapshotRepository
        -ExternalEvaluationService externalEvaluationService
        +handle(GetDashboardMetricsQuery) Optional~KpiDashboardMetrics~
    }
    class KpiHistoricalTrendQueryServiceImpl {
        -DeviceAnalyticsSnapshotRepository deviceAnalyticsSnapshotRepository
        -ExternalEvaluationService externalEvaluationService
        -TrendAnalysisDomainService trendAnalysisDomainService
        +handle(GetHistoricalTrendQuery) List~KpiTrendPoint~
    }
    class OverviewDashboardQueryServiceImpl {
        -ExternalDeviceService externalDeviceService
        -KpiLiveMetricsCache kpiLiveMetricsCache
        +handle(GetOverviewDashboardQuery) OverviewDashboardSnapshot
    }
    class AnalyticsContextFacadeImpl {
        <<interface>>
    }
    class TelemetryAnalyticKafkaConsumer {
        -KpiLiveMetricsCommandService kpiLiveMetricsCommandService
        -ObjectMapper objectMapper
        +consume(record) void
    }
    class KpiLiveMetricsCache {
        -ConcurrentHashMap~DeviceId, DeviceMetricsSnapshot~ cache
        +get(deviceId) Optional~DeviceMetricsSnapshot~
        +put(deviceId, snapshot) void
    }
    class AnalyticsSseService {
        -ConcurrentHashMap~UUID, List~SseEmitter~~ emitters
        +registerClient(deviceId) SseEmitter
        +broadcast(deviceId, data) void
    }
    class SnapshotAggregationScheduler {
        -MetricsAggregationDomainService metricsAggregationDomainService
        -DeviceAnalyticsSnapshotRepository deviceAnalyticsSnapshotRepository
        -ExternalDeviceService externalDeviceService
        -ExternalEvaluationService externalEvaluationService
        -AqiCalculationDomainService aqiCalculationDomainService
        +aggregateSnapshots() void
    }
    class ExternalDeviceService {
        <<interface>>
        +fetchAllActiveDevices() List
    }
    class ExternalEvaluationService {
        <<interface>>
        +fetchHourlyTelemetryAggregation(start, end) List
        +getLatestEvaluationRecordedAt(deviceId) Optional~Instant~
    }
}

namespace domain {
    class DeviceAnalyticsSnapshot {
        -UUID id
        -DeviceId deviceId
        -Instant timeWindowStart
        -Instant timeWindowEnd
        -Double averageCo2
        -Double averagePm2_5
        -Double averageTemperature
        -Double averageHumidity
        -AirQualityIndex calculatedAqi
    }
    class AirQualityIndex {
        +Double value
        +AqiCategory category
    }
    class AqiCategory {
        <<enumeration>>
        GOOD
        MODERATE
        UNHEALTHY_FOR_SENSITIVE_GROUPS
        UNHEALTHY
        VERY_UNHEALTHY
        HAZARDOUS
    }
    class DeviceId {
        +UUID value
    }
    class KpiDashboardMetrics {
        +DeviceId deviceId
        +Double currentCo2
        +Double currentPm2_5
        +Double currentTemperature
        +Double currentHumidity
        +AirQualityIndex calculatedAqi
    }
    class KpiTrendPoint {
        +Instant timestamp
        +Double co2
        +Double pm2_5
        +Double temperature
        +Double humidity
    }
    class AqiCalculationDomainService {
        <<interface>>
        +calculateAqi(pm2_5) AirQualityIndex
    }
    class AqiCalculationDomainServiceImpl {
        +calculateAqi(pm2_5) AirQualityIndex
    }
    class MetricsAggregationDomainService {
        <<interface>>
        +aggregate(snapshots) AggregatedMetrics
    }
    class MetricsAggregationDomainServiceImpl {
        -AqiCalculationDomainService aqiCalculationDomainService
        +aggregate(snapshots) AggregatedMetrics
    }
    class TrendAnalysisDomainService {
        <<interface>>
        +calculateTrend(currentValue, previousValue) MetricTrend
    }
    class TrendAnalysisDomainServiceImpl {
        +calculateTrend(currentValue, previousValue) MetricTrend
    }
    class DeviceAnalyticsSnapshotRepository {
        <<interface>>
        +save(snapshot) DeviceAnalyticsSnapshot
        +findByDeviceIdAndTimeWindowStartBetween(deviceId, start, end) List~DeviceAnalyticsSnapshot~
    }
}

namespace infrastructure {
    class JpaDeviceAnalyticsSnapshotRepository {
        <<interface>>
    }
}

AnalyticsController --> KpiDashboardMetricsQueryServiceImpl : uses
AnalyticsController --> KpiHistoricalTrendQueryServiceImpl : uses
AnalyticsController --> AnalyticsSseService : uses

AnalyticsOverviewController --> OverviewDashboardQueryServiceImpl : uses

KpiLiveMetricsCommandServiceImpl --> KpiLiveMetricsCache : uses
KpiLiveMetricsCommandServiceImpl --> AnalyticsSseService : uses

KpiDashboardMetricsQueryServiceImpl --> DeviceAnalyticsSnapshotRepository : uses
KpiDashboardMetricsQueryServiceImpl --> KpiLiveMetricsCache : uses
KpiDashboardMetricsQueryServiceImpl --> ExternalEvaluationService : uses

KpiHistoricalTrendQueryServiceImpl --> DeviceAnalyticsSnapshotRepository : uses
KpiHistoricalTrendQueryServiceImpl --> ExternalEvaluationService : uses
KpiHistoricalTrendQueryServiceImpl --> TrendAnalysisDomainService : uses

OverviewDashboardQueryServiceImpl --> ExternalDeviceService : uses
OverviewDashboardQueryServiceImpl --> KpiLiveMetricsCache : uses

TelemetryAnalyticKafkaConsumer --> KpiLiveMetricsCommandServiceImpl : uses

SnapshotAggregationScheduler --> MetricsAggregationDomainService : uses
SnapshotAggregationScheduler --> DeviceAnalyticsSnapshotRepository : uses
SnapshotAggregationScheduler --> ExternalDeviceService : uses
SnapshotAggregationScheduler --> ExternalEvaluationService : uses
SnapshotAggregationScheduler --> AqiCalculationDomainService : uses

DeviceAnalyticsSnapshot --> DeviceId : contains
DeviceAnalyticsSnapshot --> AirQualityIndex : contains
AirQualityIndex --> AqiCategory : contains

AqiCalculationDomainServiceImpl ..|> AqiCalculationDomainService : implements
MetricsAggregationDomainServiceImpl ..|> MetricsAggregationDomainService : implements
TrendAnalysisDomainServiceImpl ..|> TrendAnalysisDomainService : implements
JpaDeviceAnalyticsSnapshotRepository ..|> DeviceAnalyticsSnapshotRepository : implements
```

---

## Layered Diagrams

### 1. Interfaces Layer

```mermaid
classDiagram
class AnalyticsController {
    -KpiDashboardMetricsQueryService kpiDashboardMetricsQueryService
    -KpiHistoricalTrendQueryService kpiHistoricalTrendQueryService
    -AnalyticsSseService analyticsSseService
    +getLiveMetrics(deviceId) ResponseEntity
    +streamLiveMetrics(deviceId) SseEmitter
    +getHistoricalMetrics(deviceId, period, startDate, endDate) ResponseEntity
    +getTrends(deviceId, period, startDate, endDate, limit) ResponseEntity
}
class AnalyticsOverviewController {
    -OverviewDashboardQueryService overviewDashboardQueryService
    +getOverviewDashboard() ResponseEntity
}
class AnalyticsContextFacade {
    <<interface>>
}
```

### 2. Application Layer

```mermaid
classDiagram
class KpiLiveMetricsCommandServiceImpl {
    -KpiLiveMetricsCache kpiLiveMetricsCache
    -AnalyticsSseService analyticsSseService
    +handle(ProcessTelemetryAnalyticCommand) void
}
class KpiDashboardMetricsQueryServiceImpl {
    -KpiLiveMetricsCache kpiLiveMetricsCache
    -DeviceAnalyticsSnapshotRepository deviceAnalyticsSnapshotRepository
    -ExternalEvaluationService externalEvaluationService
    +handle(GetDashboardMetricsQuery) Optional~KpiDashboardMetrics~
}
class KpiHistoricalTrendQueryServiceImpl {
    -DeviceAnalyticsSnapshotRepository deviceAnalyticsSnapshotRepository
    -ExternalEvaluationService externalEvaluationService
    -TrendAnalysisDomainService trendAnalysisDomainService
    +handle(GetHistoricalTrendQuery) List~KpiTrendPoint~
}
class OverviewDashboardQueryServiceImpl {
    -ExternalDeviceService externalDeviceService
    -KpiLiveMetricsCache kpiLiveMetricsCache
    +handle(GetOverviewDashboardQuery) OverviewDashboardSnapshot
}
class AnalyticsContextFacadeImpl {
    <<interface>>
}
class TelemetryAnalyticKafkaConsumer {
    -KpiLiveMetricsCommandService kpiLiveMetricsCommandService
    -ObjectMapper objectMapper
    +consume(record) void
}
class KpiLiveMetricsCache {
    -ConcurrentHashMap~DeviceId, DeviceMetricsSnapshot~ cache
    +get(deviceId) Optional~DeviceMetricsSnapshot~
    +put(deviceId, snapshot) void
}
class AnalyticsSseService {
    -ConcurrentHashMap~UUID, List~SseEmitter~~ emitters
    +registerClient(deviceId) SseEmitter
    +broadcast(deviceId, data) void
}
class SnapshotAggregationScheduler {
    -MetricsAggregationDomainService metricsAggregationDomainService
    -DeviceAnalyticsSnapshotRepository deviceAnalyticsSnapshotRepository
    -ExternalDeviceService externalDeviceService
    -ExternalEvaluationService externalEvaluationService
    -AqiCalculationDomainService aqiCalculationDomainService
    +aggregateSnapshots() void
}
class ExternalDeviceService {
    <<interface>>
    +fetchAllActiveDevices() List
}
class ExternalEvaluationService {
    <<interface>>
    +fetchHourlyTelemetryAggregation(start, end) List
    +getLatestEvaluationRecordedAt(deviceId) Optional~Instant~
}

KpiLiveMetricsCommandServiceImpl --> KpiLiveMetricsCache : uses
KpiLiveMetricsCommandServiceImpl --> AnalyticsSseService : uses
OverviewDashboardQueryServiceImpl --> ExternalDeviceService : uses
OverviewDashboardQueryServiceImpl --> KpiLiveMetricsCache : uses
TelemetryAnalyticKafkaConsumer --> KpiLiveMetricsCommandServiceImpl : uses
KpiDashboardMetricsQueryServiceImpl --> KpiLiveMetricsCache : uses
KpiDashboardMetricsQueryServiceImpl --> ExternalEvaluationService : uses
KpiHistoricalTrendQueryServiceImpl --> ExternalEvaluationService : uses
SnapshotAggregationScheduler --> ExternalDeviceService : uses
SnapshotAggregationScheduler --> ExternalEvaluationService : uses
```

### 3. Domain Layer

```mermaid
classDiagram
class DeviceAnalyticsSnapshot {
    -UUID id
    -DeviceId deviceId
    -Instant timeWindowStart
    -Instant timeWindowEnd
    -Double averageCo2
    -Double averagePm2_5
    -Double averageTemperature
    -Double averageHumidity
    -AirQualityIndex calculatedAqi
}
class AirQualityIndex {
    +Double value
    +AqiCategory category
}
class AqiCategory {
    <<enumeration>>
    GOOD
    MODERATE
    UNHEALTHY_FOR_SENSITIVE_GROUPS
    UNHEALTHY
    VERY_UNHEALTHY
    HAZARDOUS
}
class DeviceId {
    +UUID value
}
class KpiDashboardMetrics {
    +DeviceId deviceId
    +Double currentCo2
    +Double currentPm2_5
    +Double currentTemperature
    +Double currentHumidity
    +AirQualityIndex calculatedAqi
}
class KpiTrendPoint {
    +Instant timestamp
    +Double co2
    +Double pm2_5
    +Double temperature
    +Double humidity
}
class AqiCalculationDomainService {
    <<interface>>
    +calculateAqi(pm2_5) AirQualityIndex
}
class AqiCalculationDomainServiceImpl {
    +calculateAqi(pm2_5) AirQualityIndex
}
class MetricsAggregationDomainService {
    <<interface>>
    +aggregate(snapshots) AggregatedMetrics
}
class MetricsAggregationDomainServiceImpl {
    -AqiCalculationDomainService aqiCalculationDomainService
    +aggregate(snapshots) AggregatedMetrics
}
class TrendAnalysisDomainService {
    <<interface>>
    +calculateTrend(currentValue, previousValue) MetricTrend
}
class TrendAnalysisDomainServiceImpl {
    +calculateTrend(currentValue, previousValue) MetricTrend
}
class DeviceAnalyticsSnapshotRepository {
    <<interface>>
    +save(snapshot) DeviceAnalyticsSnapshot
    +findByDeviceIdAndTimeWindowStartBetween(deviceId, start, end) List~DeviceAnalyticsSnapshot~
}

DeviceAnalyticsSnapshot --> DeviceId : contains
DeviceAnalyticsSnapshot --> AirQualityIndex : contains
AirQualityIndex --> AqiCategory : contains
AqiCalculationDomainServiceImpl ..|> AqiCalculationDomainService : implements
MetricsAggregationDomainServiceImpl ..|> MetricsAggregationDomainService : implements
TrendAnalysisDomainServiceImpl ..|> TrendAnalysisDomainService : implements
```

### 4. Infrastructure Layer

```mermaid
classDiagram
class JpaDeviceAnalyticsSnapshotRepository {
    <<interface>>
}
```
