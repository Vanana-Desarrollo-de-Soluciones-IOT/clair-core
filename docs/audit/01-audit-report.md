# Audit Report — 204b7cc — 2026-09-04

Scope: `src/main/java/com/claircore` (381 files, 7 bounded contexts + `shared`). Method: the procedure in `AUDIT-PROMPT` §5, applied by static import sweeps plus source reading. No code was changed. Every claim below cites a path; version and configuration facts were read from `pom.xml` and `application.yml`, not recalled.

## Summary

**BLOCKER: 90 | MAJOR: 41 | MINOR: 19 | INFO: 9 | UNCLASSIFIED: 14**

The single structural problem: **every persistent domain object is simultaneously the aggregate and the JPA entity.** 48 files under `domain/` carry 80 `jakarta.persistence` / `spring-data-jpa` imports, the shared auditing base class (`shared/domain/model/entities/AuditableModel`) is a `@MappedSuperclass`, and no context has a `domain/repositories` port for its aggregates (notifications is the sole partial exception). Because there is no port, every application service, every ACL facade implementation, and two controllers inject Spring Data `JpaRepository` interfaces from `infrastructure/`. The persistence-entity / assembler / adapter layer of the target architecture (§1) does not exist anywhere in the codebase, so Steps 5, 6 and the first half of the branch plan are green-field rather than corrective. Secondary problems: cross-context leakage bypasses the ACL in 9 places (shared imports a concrete context; contexts import each other's `domain.model.events` and value objects), inbound ports live under `domain/services` instead of `application/*services`, and the unauthenticated telemetry and presence ingestion endpoints are a security defect independent of DDD.

Two positives worth recording so later branches do not undo them: (1) aggregates already enforce invariants in constructors and expose behaviour methods, not setters (e.g. `device/domain/model/entities/DeviceAssignment.java`, `alerting/domain/model/entities/Alert.java`); (2) cross-context *calls* are correctly routed through `interfaces/acl/*ContextFacade` + `outboundservices/acl/External*Service` in every context. The refactor is a reshaping of packaging and persistence, not a rewrite of business rules.

---

## A. Invariant violations (§4.1)

80 import lines across 48 files. Every one is a `BLOCKER`. Grouped by file; line numbers are those of the import statement.

| # | File | Line(s) | Import | Severity |
|---|---|---|---|---|
| 1 | `shared/domain/model/entities/AuditableModel.java` | 3,4,5,8 | `jakarta.persistence.Column`, `EntityListeners`, `MappedSuperclass`, `o.s.data.jpa.domain.support.AuditingEntityListener` | BLOCKER |
| 2 | `alerting/domain/model/entities/Alert.java` | 7,8 | `jakarta.persistence.*`, `AuditingEntityListener` | BLOCKER |
| 3 | `analytics/domain/model/entities/DeviceAnalyticsSnapshot.java` | 6,7 | `jakarta.persistence.*`, `AuditingEntityListener` | BLOCKER |
| 4 | `analytics/domain/model/entities/DeviceDailySummary.java` | 8,9 | `jakarta.persistence.*`, `AuditingEntityListener` | BLOCKER |
| 5 | `analytics/domain/model/entities/DeviceMonthlySummary.java` | 8,9 | `jakarta.persistence.*`, `AuditingEntityListener` | BLOCKER |
| 6 | `analytics/domain/model/valueobjects/AirQualityIndex.java` | 3,4,5 | `Embeddable`, `EnumType`, `Enumerated` | BLOCKER |
| 7 | `analytics/domain/model/valueobjects/AqiCategoryBreakdown.java` | 3 | `Embeddable` | BLOCKER |
| 8 | `analytics/domain/model/valueobjects/DeviceId.java` | 3 | `Embeddable` | BLOCKER |
| 9 | `analytics/domain/model/valueobjects/MetricStats.java` | 3 | `Embeddable` | BLOCKER |
| 10 | `analytics/domain/model/valueobjects/MetricTrend.java` | 3 | `Embeddable` | BLOCKER |
| 11 | `analytics/domain/services/AqiCalculationDomainServiceImpl.java` | 6 | `o.s.stereotype.Service` | BLOCKER |
| 12 | `analytics/domain/services/MetricsAggregationDomainServiceImpl.java` | 7 | `o.s.stereotype.Service` | BLOCKER |
| 13 | `analytics/domain/services/TrendAnalysisDomainServiceImpl.java` | 5 | `o.s.stereotype.Service` | BLOCKER |
| 14 | `billing/domain/model/aggregates/PaymentRecord.java` | 8,10 | `jakarta.persistence.*`, `AuditingEntityListener` | BLOCKER |
| 15 | `billing/domain/model/aggregates/UserPlan.java` | 6,8 | `jakarta.persistence.*`, `AuditingEntityListener` | BLOCKER |
| 16 | `billing/domain/model/valueobjects/Money.java` | 3,4 | `Column`, `Embeddable` | BLOCKER |
| 17 | `billing/domain/model/valueobjects/UserId.java` | 3 | `Embeddable` | BLOCKER |
| 18 | `device/domain/model/entities/Device.java` | 5,6 | `jakarta.persistence.*`, `AuditingEntityListener` | BLOCKER |
| 19 | `device/domain/model/entities/DeviceAssignment.java` | 7,8 | `jakarta.persistence.*`, `AuditingEntityListener` | BLOCKER |
| 20 | `device/domain/model/entities/DeviceCommand.java` | 6,7 | `jakarta.persistence.*`, `AuditingEntityListener` | BLOCKER |
| 21 | `device/domain/model/entities/Organization.java` | 5,6 | `jakarta.persistence.*`, `AuditingEntityListener` | BLOCKER |
| 22 | `device/domain/model/entities/Space.java` | 5,6 | `jakarta.persistence.*`, `AuditingEntityListener` | BLOCKER |
| 23 | `device/domain/model/valueobjects/ApiKey.java` | 3 | `Embeddable` | BLOCKER |
| 24 | `device/domain/model/valueobjects/ClaimToken.java` | 3 | `Embeddable` | BLOCKER |
| 25 | `device/domain/model/valueobjects/DeviceMetricThresholdConfiguration.java` | 3,4,5 | `Embeddable`, `EnumType`, `Enumerated` | BLOCKER |
| 26 | `device/domain/model/valueobjects/DeviceType.java` | 3 | `Embeddable` | BLOCKER |
| 27 | `device/domain/model/valueobjects/HardwareId.java` | 3 | `Embeddable` | BLOCKER |
| 28 | `device/domain/model/valueobjects/UserId.java` | 3,4 | `Column`, `Embeddable` | BLOCKER |
| 29 | `evaluation/domain/model/entities/TelemetryEvaluation.java` | 5,6 | `jakarta.persistence.*`, `AuditingEntityListener` | BLOCKER |
| 30 | `evaluation/domain/model/valueobjects/AirQuality.java` | 3 | `Embeddable` | BLOCKER |
| 31 | `evaluation/domain/model/valueobjects/Connectivity.java` | 3 | `Embeddable` | BLOCKER |
| 32 | `evaluation/domain/model/valueobjects/DeviceId.java` | 3 | `Embeddable` | BLOCKER |
| 33 | `evaluation/domain/model/valueobjects/Location.java` | 3 | `Embeddable` | BLOCKER |
| 34 | `evaluation/domain/model/valueobjects/ParticulateMatter.java` | 3 | `Embeddable` | BLOCKER |
| 35 | `iam/domain/model/entities/User.java` | 10 | `jakarta.persistence.*` | BLOCKER |
| 36 | `iam/domain/model/valueobjects/EmailAddress.java` | 5 | `Embeddable` | BLOCKER |
| 37 | `iam/domain/model/valueobjects/GoogleIdToken.java` | 3 | `Embeddable` | BLOCKER |
| 38 | `iam/domain/model/valueobjects/GoogleUserId.java` | 3 | `Embeddable` | BLOCKER |
| 39 | `iam/domain/model/valueobjects/Password.java` | 3 | `Embeddable` | BLOCKER |
| 40 | `iam/domain/model/valueobjects/RegistrationSessionId.java` | 5 | `Embeddable` | BLOCKER |
| 41 | `iam/domain/model/valueobjects/TokenJti.java` | 5 | `Embeddable` | BLOCKER |
| 42 | `iam/domain/model/valueobjects/UserId.java` | 3,4 | `Column`, `Embeddable` | BLOCKER |
| 43 | `iam/domain/model/valueobjects/VerificationCode.java` | 5 | `Embeddable` | BLOCKER |
| 44 | `notifications/domain/model/entities/EmailLog.java` | 7–11 | `Column`, `Embedded`, `Entity`, `Id`, `Table` | BLOCKER |
| 45 | `notifications/domain/model/entities/PushNotificationLog.java` | 4–7 | `Column`, `Entity`, `Id`, `Table` | BLOCKER |
| 46 | `notifications/domain/model/valueobjects/EmailContent.java` | 3,4 | `Column`, `Embeddable` | BLOCKER |
| 47 | `notifications/domain/model/valueobjects/EmailRecipient.java` | 3,4 | `Column`, `Embeddable` | BLOCKER |
| 48 | `notifications/domain/model/valueobjects/EmailSubject.java` | 3,4 | `Column`, `Embeddable` | BLOCKER |

**Other framework imports in `domain/` (§4.1 says "report any other Spring import"):**

| # | File | Import | Severity | Note |
|---|---|---|---|---|
| 49 | `alerting/domain/services/AlertQueryService.java`, `device/domain/services/DeviceQueryService.java`, `evaluation/domain/services/TelemetryEvaluationQueryService.java`, `notifications/domain/services/PushNotificationHistoryQueryService.java`, `notifications/domain/repositories/PushNotificationHistoryRepository.java` | `o.s.data.domain.Page` | MAJOR | Spring Data Commons pagination type in a domain port. Not JPA, but it forces every implementer to be Spring Data. Target: a domain `PageResult<T>` record in `shared/domain`. |
| 50 | `alerting/domain/model/queries/GetAlertsBy{Device,Owner,Space}Query.java`, `notifications/domain/model/queries/GetPushNotificationHistoryQuery.java`, `notifications/domain/repositories/PushNotificationHistoryRepository.java` | `o.s.data.domain.Pageable` | MAJOR | Same; a query record should carry `page`/`size` ints (as `evaluation/domain/model/queries/GetEvaluationsByDeviceQuery` already does). |
| 51 | `billing/domain/model/events/SubscriptionPaidEvent.java`, `iam/domain/model/events/UserRegisteredEvent.java` | `o.s.context.ApplicationEvent` | MAJOR | Domain events extend a Spring class. Target: plain records; `AbstractDomainAggregateRoot.registerEvent` handles publication. |
| 52 | `billing/domain/model/aggregates/{PaymentRecord,UserPlan}.java` | `o.s.data.domain.AbstractAggregateRoot` | INFO | This is the documented exception, but it is applied directly to two aggregates rather than through a `shared/domain/model/aggregates/AbstractDomainAggregateRoot`. Only billing uses it; the other 15 aggregates have no event registration at all. |
| 53 | `shared/domain/model/entities/AuditableModel.java` | `o.s.data.annotation.{CreatedDate,LastModifiedDate}` | BLOCKER (part of #1) | Auditing annotations are infrastructure; the whole class moves to `shared/infrastructure/persistence/jpa/entities/AuditableAbstractPersistenceEntity`. |
| 54 | `iam/domain/model/entities/{User,RegistrationSession,TokenSession}.java`, `iam/domain/model/valueobjects/{EmailAddress,RegistrationSessionId,TokenJti,VerificationCode}.java` | `com.fasterxml.jackson.annotation.*` | MAJOR | Serialisation concern in domain so Redis can round-trip the object. Target: a Redis persistence DTO under `iam/infrastructure/persistence/redis/entities` with its own assembler. |
| 55 | `iam/domain/model/valueobjects/EmailAddress.java` | `jakarta.validation.constraints.Email` | MINOR | Bean Validation on a record that already self-validates in its compact constructor. Redundant; drop. |
| 56 | `notifications/domain/model/entities/{EmailLog,PushNotificationLog}.java` | `org.hibernate.annotations.UuidGenerator` | BLOCKER (part of #44/#45) | Hibernate-specific, moves with the entity split. |

No `lombok.*` imports exist anywhere in `src/main` (`pom.xml` has no Lombok dependency). §4.2 row 1 is clean.

---

## B. Layer direction violations

### B.1 `application` → `infrastructure` (MAJOR each; §4.2 "persistence leaked past the port")

Every application service and ACL facade implementation injects a Spring Data repository from `infrastructure/persistence/jpa/repositories`. This is the mechanical consequence of finding A: there is no port to inject instead. 44 files. Listed by context; each import line is a distinct edge.

| Context | Files (all under `<ctx>/application/`) | Infrastructure type imported |
|---|---|---|
| alerting | `acl/AlertingContextFacadeImpl` (l.6), `internal/commandservices/AlertCommandServiceImpl` (l.13), `internal/commandservices/EdgeAlertAcknowledgementService` (l.5), `internal/queryservices/AlertQueryServiceImpl` (l.10) | `AlertRepository` |
| alerting | `internal/outboundservices/acl/AlertIncidentsChangedPublisher` (l.4) | `shared.infrastructure.edge.EdgeEventPublisher` |
| analytics | `acl/AnalyticsContextFacadeImpl` (l.4), `internal/queryservices/{DailyReport,KpiDashboardMetrics,KpiHistoricalTrend,MonthlyReport,OverviewDashboard}QueryServiceImpl`, `internal/services/{DailyReportAggregationService,MonthlyReportAggregationService,SnapshotAggregationScheduler}` | `DeviceAnalyticsSnapshotRepository`, `DeviceDailySummaryRepository`, `DeviceMonthlySummaryRepository` |
| billing | `acl/BillingContextFacadeImpl` (l.7), `internal/commandservices/SubscriptionCommandServiceImpl` (l.10,11), `internal/eventhandlers/{SubscriptionPaid,UserRegistered}EventHandler`, `internal/queryservices/SubscriptionQueryServiceImpl` (l.10,11) | `UserPlanRepository`, `PaymentRecordRepository` |
| device | `acl/ThresholdContextFacadeImpl` (l.7), `internal/commandservices/{Device,DeviceControl,DevicePresence,DeviceThreshold,Organization,Space}CommandServiceImpl`, `internal/commandservices/EdgeCommandAcknowledgementService`, `internal/queryservices/{Device,DeviceCommand,DeviceStatus,DeviceThreshold}QueryServiceImpl` | `DeviceRepository`, `DeviceAssignmentRepository`, `DeviceCommandRepository`, `OrganizationRepository`, `SpaceRepository` |
| device | `internal/outboundservices/acl/{DeviceCommandsPendingPublisher,ProvisioningDevicesChangedPublisher}` | `shared.infrastructure.edge.EdgeEventPublisher` |
| evaluation | `internal/commandservices/TelemetryEvaluationCommandServiceImpl` (l.7), `internal/queryservices/TelemetryEvaluationQueryServiceImpl` (l.7) | `TelemetryEvaluationRepository` |
| iam | `internal/commandservices/{GoogleAuthentication,User}CommandServiceImpl`, `internal/queryservices/UserQueryServiceImpl` | `UserRepository` |
| iam | `internal/commandservices/{Token,User}CommandServiceImpl`, `internal/queryservices/TokenQueryServiceImpl` | `redis.repositories.{TokenSession,RegistrationSession}Repository` (concrete classes, no interface) |
| iam | `internal/commandservices/TokenCommandServiceImpl` (l.10), `internal/queryservices/TokenQueryServiceImpl` (l.8) | `infrastructure.tokens.jwt.JwtTokenEncoder` (concrete class; should be an outbound `TokenService` port) |
| iam | `internal/commandservices/GoogleOAuthCallbackApplicationService` (l.7) | `infrastructure.oauth.google.GoogleAuthorizationCodeTokenClient` (concrete) |

Also `application` → `interfaces` (inverted, MAJOR):

| # | From | To |
|---|---|---|
| B1-a | `alerting/application/internal/commandservices/EdgeAlertAcknowledgementService.java:6` | `alerting.interfaces.rest.resources.EdgeAlertAckRequest` (a REST DTO used as a service parameter) |
| B1-b | `device/application/internal/commandservices/EdgeCommandAcknowledgementService.java:7` | `device.interfaces.rest.resources.EdgeCommandAckRequest` |

`application/acl/*FacadeImpl` importing `<own>.interfaces.acl.*Facade` is **not** reported: the facade interface is the published contract and the reference places the implementation exactly there.

### B.2 `interfaces` → `infrastructure` (BLOCKER each)

| # | From file | Import | Severity | Note |
|---|---|---|---|---|
| B2-1 | `alerting/interfaces/rest/controllers/EdgeAlertController.java:5` | `alerting.infrastructure.persistence.jpa.repositories.AlertRepository` | BLOCKER | Controller queries the database directly (`repository.findPendingForEdge(...)`). |
| B2-2 | `device/interfaces/rest/controllers/DeviceRosterController.java:3` | `device.infrastructure.persistence.jpa.repositories.DeviceRepository` | BLOCKER | Controller queries the database directly and also re-implements token auth inline (see J). |
| B2-3 | `iam/interfaces/rest/controllers/AuthenticationController.java:13` | `iam.infrastructure.oauth.google.GoogleOAuthStateManager` | BLOCKER | Controller depends on a concrete infrastructure class. |
| B2-4 | `alerting/interfaces/rest/controllers/AlertController.java:12` | `iam.infrastructure.tokens.jwt.JwtAuthenticationFilter` | BLOCKER | Only for the `USER_ID_ATTRIBUTE` constant. Cross-context **and** cross-layer. |
| B2-5 | `analytics/interfaces/rest/controllers/AnalyticsOverviewController.java:7` | same | BLOCKER | same |
| B2-6 | `analytics/interfaces/rest/controllers/ReportController.java:13` | same | BLOCKER | same |
| B2-7 | `evaluation/interfaces/rest/controllers/TelemetryEvaluationController.java:14` | same | BLOCKER | same |
| B2-8 | `notifications/interfaces/rest/controllers/NotificationController.java:6` | same | BLOCKER | same |

Fix for B2-4..8 is one move: a `shared/interfaces/rest/security/AuthenticatedUser` resolver (or `@AuthenticationPrincipal`) so controllers never name a filter class.

### B.3 `interfaces` naming a persistence entity (§4.2 row 3, MAJOR)

Because aggregate and entity are the same class today, every controller that returns a domain object technically "names a persistence entity". This is reported once, structurally, rather than per file: it resolves automatically when branch 2 splits the classes. Notable cases where a controller returns raw entities without a transform: `alerting/interfaces/rest/controllers/EdgeAlertController` (hand-built `Map<String,Object>`), `device/interfaces/rest/controllers/EdgeCommandController` (same), `device/interfaces/rest/controllers/DeviceRosterController`.

### B.4 `domain` → `application|infrastructure|interfaces`

None. Clean.

---

## C. Context boundary violations

Rule: context A may import context B only via `B.interfaces.acl.*`.

| # | From (file) | To context | Import | Via ACL? | Severity |
|---|---|---|---|---|---|
| C1 | `shared/interfaces/rest/exceptions/GlobalExceptionHandler.java:13` | analytics | `analytics.domain.exceptions.DeviceTelemetryUnavailableException` | No | **BLOCKER** (shared → concrete context = cycle) |
| C2 | `alerting/application/internal/commandservices/AlertCommandServiceImpl.java:14` | device | `device.domain.model.valueobjects.DeviceMetricThresholdConfiguration` | No | BLOCKER |
| C3 | `alerting/application/internal/outboundservices/acl/ExternalAlertingThresholdService.java:3` | device | same VO | No | BLOCKER |
| C4 | `device/interfaces/acl/ThresholdContextFacade.java` | (publishes) | exposes `device.domain.model.valueobjects.{DeviceMetricThresholdConfiguration,MetricThreshold}` in its signature | — | BLOCKER (root cause of C2/C3: the facade leaks domain types instead of a DTO like `OrganizationSummary`) |
| C5 | `alerting/application/internal/inboundservices/acl/AlertingTelemetryRecordedEventListener.java:5` | evaluation | `evaluation.domain.model.events.TelemetryRecordedEvent` | No | BLOCKER |
| C6 | `analytics/application/internal/inboundservices/acl/TelemetryAnalyticEventListener.java:6` | evaluation | same | No | BLOCKER |
| C7 | `analytics/application/internal/queryservices/OverviewDashboardQueryServiceImpl.java:3` | alerting | `alerting.domain.model.valueobjects.AlertStatus` | No | BLOCKER |
| C8 | `alerting/interfaces/acl/AlertingContextFacade.java` | (publishes) | `getRecentAlertsByOwnerId(UUID, List<AlertStatus>, int)` exposes a domain enum | — | BLOCKER (root cause of C7) |
| C9 | `notifications/application/internal/inboundservices/acl/AlertIncidentChangedEventListener.java:3,4` | alerting | `alerting.domain.model.valueobjects.AlertStatus`, `alerting.domain.model.events.AlertIncidentChangedEvent` | No | BLOCKER |
| C10 | `billing/application/internal/eventhandlers/UserRegisteredEventHandler.java:6` | iam | `iam.domain.model.events.UserRegisteredEvent` | No | BLOCKER |
| C11 | `alerting/infrastructure/persistence/jpa/repositories/AlertRepository.java:640,646` | device | JPQL `JOIN Device d ON d.id = a.deviceId` | No | BLOCKER (a database-level join across contexts; the alerting schema depends on the device table name and entity mapping) |
| C12 | `analytics/application/internal/services/DailyReportAggregationService.java:57-62` | evaluation | raw SQL against `telemetry_evaluations` via `JdbcTemplate` | No | BLOCKER (analytics reads evaluation's table directly; `evaluation/application/acl/EvaluationContextFacadeImpl.getHourlyTelemetryAggregation` does the same but at least inside evaluation) |
| C13 | B2-4..B2-8 above | iam | `iam.infrastructure.tokens.jwt.JwtAuthenticationFilter` | No | BLOCKER (already counted in B) |

**Pattern.** C5, C6, C9, C10 are all the same defect: an integration event is published as the *upstream's domain event record* and consumed by the downstream's listener. Each downstream context already has an unused `TelemetryRecordedIntegrationEvent` record (`alerting/…/inboundservices/acl`, `analytics/…/inboundservices/acl`, `evaluation/…/inboundservices/acl`; 0 consumers each) that was clearly intended as the ACL translation type and never wired. The target is: upstream publishes `<Ctx>.interfaces.events.XxxIntegrationEvent`; downstream listener imports only that.

---

## D. Completeness matrix

Legend: ✅ present, ❌ missing, ⚠️ present but misplaced/conflated, — n/a. "Aggregate" ⚠️ means the class exists but is the JPA entity (finding A). "Port" is `domain/repositories`. Severities per §5 Step 5.

| Aggregate (ctx) | Agg | VOs | Cmds | Queries | Port | CmdSvc | CmdImpl | QrySvc | QryImpl | PersEntity | SpringRepo | Assembler | Adapter | Controller | Resource | Transform |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `Alert` (alerting) | ⚠️ `entities/` | ✅ | ✅ | ✅ ⚠️Pageable | ❌ | ⚠️ `domain/services` | ✅ | ⚠️ `domain/services` | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ (`AlertResponse.from` static on the resource) |
| `DeviceAnalyticsSnapshot` (analytics) | ⚠️ | ✅ | — (written by scheduler) | ✅ | ❌ | — | — | ⚠️ | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ |
| `DeviceDailySummary` (analytics) | ⚠️ | ✅ | ❌ (scheduler writes, no command) | ✅ | ❌ | ❌ | ⚠️ `internal/services/DailyReportAggregationService` | ⚠️ | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ |
| `DeviceMonthlySummary` (analytics) | ⚠️ | ✅ | ❌ | ✅ | ❌ | ❌ | ⚠️ `internal/services/MonthlyReportAggregationService` | ⚠️ | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ |
| `UserPlan` (billing) | ⚠️ `aggregates/` | ✅ | ✅ | ✅ | ❌ | ⚠️ | ✅ | ⚠️ | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ |
| `PaymentRecord` (billing) | ⚠️ `aggregates/` | ✅ | ✅ | ✅ | ❌ | ⚠️ | ✅ | ⚠️ | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ (webhook) | ✅ | ✅ |
| `Device` (device) | ⚠️ | ✅ | ✅ | ✅ | ❌ | ⚠️ | ✅ | ⚠️ | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ (`from` statics on `DeviceResponse`) |
| `DeviceAssignment` (device) | ⚠️ | ✅ | ✅ | ✅ | ❌ | ⚠️ | ✅ | ⚠️ | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ |
| `DeviceCommand` (device) | ⚠️ | ✅ | ✅ | ✅ | ❌ | ⚠️ | ✅ | ⚠️ | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ |
| `Organization` (device) | ⚠️ | ✅ | ✅ | ✅ | ❌ | ⚠️ | ✅ | ⚠️ (folded into `DeviceQueryService`) | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ |
| `Space` (device) | ⚠️ | ✅ | ✅ | ✅ | ❌ | ⚠️ | ✅ | ⚠️ (folded into `DeviceQueryService`) | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ |
| `TelemetryEvaluation` (evaluation) | ⚠️ | ✅ | ✅ | ✅ | ❌ | ⚠️ | ✅ | ⚠️ | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ |
| `User` (iam) | ⚠️ | ✅ ⚠️Jackson | ✅ | ✅ | ❌ | ⚠️ | ✅ | ⚠️ | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ |
| `RegistrationSession` (iam, Redis) | ✅ record | ✅ | ✅ | — | ❌ | ⚠️ | ✅ (in `UserCommandServiceImpl`) | — | — | ❌ (JSON of domain record) | ⚠️ concrete class | ❌ | ❌ | ✅ | ✅ | ✅ |
| `TokenSession` (iam, Redis) | ✅ record | ✅ | ✅ | ✅ | ❌ | ⚠️ | ✅ | ⚠️ | ✅ | ❌ | ⚠️ concrete class | ❌ | ❌ | ✅ | ✅ | ✅ |
| `EmailLog` (notifications) | ⚠️ | ✅ | ✅ | — | ⚠️ `EmailLogPersistence` (save only) | ⚠️ | ✅ | — | — | ❌ | ✅ (extends the port) | ❌ | ❌ (Spring Data interface *is* the adapter) | — | — | — |
| `PushNotificationLog` (notifications) | ⚠️ | ✅ | ❌ (written inside an event listener) | ✅ ⚠️Pageable | ⚠️ `PushNotificationHistoryRepository` (leaks `Page`) | ❌ | ❌ | ⚠️ | ✅ | ❌ | ✅ (extends the port) | ❌ | ❌ | ✅ | ✅ | ❌ |

Column totals: **Port missing 15/17 (MAJOR each), PersEntity 17/17, Assembler 17/17, Adapter 17/17 (MAJOR each).** CmdSvc/QrySvc ports exist for every aggregate but sit under `domain/services` (MINOR placement; see I). Transform missing 8/17 (MINOR).

---

## E. Assembler field coverage

**No `*PersistenceAssembler` exists in the codebase**, so there is nothing to audit for field coverage. This section is therefore a **pre-audit of what branch 5 must cover**, derived from the field lists of the current dual-purpose classes. These are the fields most likely to be dropped when the split happens, by category:

| Aggregate | Fields at risk in `toPersistenceFromDomain` | Reason |
|---|---|---|
| `DeviceAssignment` | `configuration: Map<String,String>` (`@ElementCollection`) | The only collection in the model. Classic `"configuration": {}` defect. Also holds serialised threshold JSON (see `DeviceThresholdCommandServiceImpl.thresholdConfigKey`), so a dropped map silently deletes every alert threshold. |
| `DeviceAssignment`, `DeviceCommand` | `device: Device` (`@ManyToOne LAZY`) | Nested aggregate reference. The persistence entity must hold `deviceId: UUID` (or a `DevicePersistenceEntity` ref) and the assembler must not trigger a lazy load outside a session. |
| every aggregate | `auditFields.createdAt/updatedAt` | Currently inside a nested `@Embeddable` subclass of `AuditableModel` (e.g. `Device.DeviceAudit`). Once auditing moves to the persistence base class, the aggregate must still expose `createdAt`/`updatedAt` read-only for REST responses (`EdgeCommandController.toResource` reads `getAuditFields().getCreatedAt()`). |
| `Device` | `deleted: Boolean`, `factoryName` | Flags that are never set via the constructor; easy to omit from a reconstruction factory. |
| `Alert` | `resolvedAt`, `spaceName`, `deviceName` | Nullable denormalised columns. |
| `TelemetryEvaluation` | all five embedded VOs with `@AttributeOverride` | Column names (`aq_co2`, `pm_pm2_5`…) are referenced by raw SQL in two other files (`DailyReportAggregationService`, `EvaluationContextFacadeImpl`). Renaming a column during the split breaks analytics silently. Verify by diffing DDL (branch 2 "done when"). |
| `User` | `oauthProvider`, `oauthSubject`, `id` (via `rehydrate`) | `User.rehydrate(...)` already exists as a reconstruction factory: **keep it and make it the only path the assembler uses**. This is the pattern §7.1 recommends over setters. |

Identity handling note for branch 5: every aggregate uses `@GeneratedValue(strategy = UUID)` except `OutboxMessage` and `EmailLog`/`PushNotificationLog` (`@UuidGenerator`), which assign in the constructor. After the split, the aggregate should own identity generation (`UUID.randomUUID()` in the factory) and the persistence entity should **not** carry `@GeneratedValue`, otherwise the assembler must special-case null ids on insert.

---

## F. Read-side findings

| # | Finding | File | Severity |
|---|---|---|---|
| F1 | All 12 query service impls depend on Spring Data repositories, not a domain port (consequence of A). | see B.1 | MAJOR |
| F2 | `KpiDashboardMetricsQueryServiceImpl.handle` throws `DeviceTelemetryUnavailableException` from inside a `@Transactional(readOnly=true)` method for a "no data" case. Not a mutation, but an exception for a normal empty result; the sibling `Optional.empty()` path already exists for LIVE. Inconsistent. | `analytics/application/internal/queryservices/KpiDashboardMetricsQueryServiceImpl.java:108` | MINOR |
| F3 | `OverviewDashboardQueryServiceImpl.handle` is `@Transactional(readOnly=true)` but fans out to `CompletableFuture.supplyAsync` on the common pool. The transaction and its `EntityManager` are thread-bound: the async lambdas call `snapshotRepository` and the device facade **outside** the transaction, on threads with no persistence context. Works today only because each repository call opens its own short transaction; lazy relationships would throw. Also `supplyAsync` without an executor uses `ForkJoinPool.commonPool()`, ignoring `AsyncConfig.taskExecutor`. | `analytics/application/internal/queryservices/OverviewDashboardQueryServiceImpl.java:250-283` | MAJOR |
| F4 | No JPQL/SQL string concatenation with request input anywhere. `JdbcTemplate` calls use `?` placeholders. | `EvaluationContextFacadeImpl`, `DailyReportAggregationService` | INFO (clean) |
| F5 | `Optional` / `List` / `Page` are returned; no `null` returns found in query services. | — | INFO (clean) |
| F6 | `AlertingContextFacadeImpl`, `EvaluationContextFacadeImpl`, `AnalyticsContextFacadeImpl`, `ThresholdContextFacadeImpl` and `BillingContextFacadeImpl` bypass their own query services and hit the repository (or `JdbcTemplate`) directly. The facade is meant to be a thin adapter over the inbound ports. | `*/application/acl/*FacadeImpl.java` | MAJOR |
| F7 | `EvaluationContextFacade.getHourlyTelemetryAggregation` returns `List<Map<String,Object>>` — untyped rows across a context boundary. | `evaluation/interfaces/acl/EvaluationContextFacade.java` | MAJOR |
| F8 | `ThresholdContextFacadeImpl.findEnabledThresholdsByDeviceId` deserialises threshold JSON from `DeviceAssignment.configuration` with `ObjectMapper` and swallows every exception into `Optional.empty()`. A corrupted threshold row silently disables alerting for that metric. | `device/application/acl/ThresholdContextFacadeImpl.java:766-772` | MAJOR |

---

## G. `shared/` findings

| Member | Consumer count | Verdict |
|---|---|---|
| `shared/domain/model/entities/AuditableModel` | 17 (every entity) | Keep, but **move** to `shared/infrastructure/persistence/jpa/entities/AuditableAbstractPersistenceEntity`. It is a `@MappedSuperclass`; timestamps are infrastructure (§5 Step 8.4). Uses `java.util.Date`; switch to `Instant` during the move (INFO). |
| `shared/infrastructure/config/{AsyncConfig,CacheConfig,JpaAuditingConfiguration,OpenApiConfiguration,SchedulingConfig}` | app-wide | Keep. Target path per §2: `shared/infrastructure/documentation/openapi/configuration/OpenApiConfiguration` (MINOR rename). |
| `shared/infrastructure/edge/EdgeEventPublisher` | 3 (alerting, device ×2) | Keep in shared (≥2 consumers). But it is a concrete `@Service` injected by application code (B.1). Target: an outbound port `shared/application/outboundservices/EdgeNotifier` (interface) implemented here. Also: creates `new RestTemplate()` without timeouts (MAJOR; a hung edge blocks the caller's `afterCommit` hook thread). |
| `shared/infrastructure/persistence/jpa/outbox/{OutboxMessage,OutboxMessageRepository}` | **0** | Dead. Sole textual reference is a comment in `device/…/EdgeCommandAcknowledgementService`. Kafka was removed (`docs/plans/01-eliminar-kafka.md`) and the outbox with it. Delete, or revive it as the delivery guarantee for `EdgeEventPublisher` (see 04-domain-improvements-iot-ingestion.md). The table `outbox_message` still gets created by `ddl-auto: update`. |
| `shared/infrastructure/security/ServiceTokenAuthenticationFilter` | app-wide | Keep. See J for the coverage gap. |
| `shared/interfaces/rest/exceptions/GlobalExceptionHandler` | app-wide | Keep; is `@RestControllerAdvice` ✅. Path deviates from §2 (`shared/interfaces/rest/GlobalExceptionHandler`) — MINOR. Imports a concrete context (C1) — BLOCKER. Fix: analytics throws a `shared.domain.exceptions.ResourceNotFoundException` subtype, or the handler maps on a marker interface. Also returns `Map<String,Object>`; §2 expects `ErrorResource` record. |
| `shared/application/result/{Result,ApplicationError}` | — | Absent. Not a defect; exceptions are used uniformly (§7.3 is moot, see L). |
| `shared/domain/model/aggregates/AbstractDomainAggregateRoot` | — | Absent. Branch 1 deliverable. |
| `shared/infrastructure/persistence/jpa/configuration/strategy/*PhysicalNamingStrategy` | — | Absent. `application.yml` declares no `hibernate.physical_naming_strategy`, so Spring Boot's default `CamelCaseToUnderscoresNamingStrategy` applies. Verified from config, not DDL (no database available in this audit). Table names are explicit on every `@Table` except `UserPlan` and `PaymentRecord` (→ `user_plan`, `payment_record`, singular). INFO. |

Not shared, but should be: `UserId` exists three times (`billing`, `device`, `iam`) and `DeviceId` twice (`analytics`, `evaluation`) as separate `@Embeddable` records. This is **correct DDD** (each context owns its identity VO) — do **not** hoist to `shared`. INFO, recorded so nobody "deduplicates" it.

---

## H. Transaction and lifecycle findings

| # | Finding | File | Severity |
|---|---|---|---|
| H1 | `@Transactional` sits on command service impls ✅ and **not** on controllers ✅. | all `*CommandServiceImpl` | INFO (clean) |
| H2 | `@Transactional` also sits on ACL facade impls (`AlertingContextFacadeImpl` ×3) — a facade should delegate to an already-transactional query service. | `alerting/application/acl/AlertingContextFacadeImpl.java` | MINOR |
| H3 | **Self-invocation bypasses the proxy.** `DailyReportAggregationService.aggregatePreviousDay()` (`@Scheduled`, not transactional) calls `this.generateForDate()` (`@Transactional`). The annotation on `generateForDate` does nothing when invoked from the scheduler; each `dailySummaryRepository.save` runs in its own auto-commit transaction, so a crash mid-loop leaves a partial day. Same shape in `MonthlyReportAggregationService.aggregatePreviousMonth() → generateForMonth()`. Control: `SnapshotAggregationScheduler.aggregateHourlySnapshots` puts both annotations on the same method and is therefore correct. | `analytics/application/internal/services/DailyReportAggregationService.java:58-61`, `MonthlyReportAggregationService.java:52-55` | MAJOR |
| H4 | No `Result.Failure`-inside-`@Transactional` pattern exists; all failures throw. §5 Step 9.2 does not apply. | — | INFO |
| H5 | `EdgeCommandAcknowledgementService.acknowledge` and `EdgeAlertAcknowledgementService.acknowledge` return an `Outcome` enum (`NOT_FOUND`/`CONFLICT`/`OK`) instead of throwing. Because they never mutate on the non-OK paths, no rollback is expected, so this is fine. Recorded as the one place a result-enum coexists with exceptions (§7.3). | `device/…/EdgeCommandAcknowledgementService.java`, `alerting/…/EdgeAlertAcknowledgementService.java` | INFO |
| H6 | Fetch types: `DeviceAssignment.device` and `DeviceCommand.device` are `@ManyToOne(fetch = LAZY)`; `DeviceAssignment.configuration` is `@ElementCollection` (default LAZY). No `EAGER` anywhere. Trade-off: `EdgeCommandController.toResource` and `DeviceContextFacadeImpl.findDeviceIdsBySpaceId` navigate `assignment.getDevice()` — they work only because `spring.jpa.open-in-view` defaults to `true` (not set in `application.yml`). Turning OIV off (recommended for a REST API) will surface `LazyInitializationException` in those two places. | `device/domain/model/entities/DeviceAssignment.java:81`, `DeviceCommand.java:193` | INFO |
| H7 | `@EventListener` (synchronous, same thread, same transaction) is used for all in-process integration: `TelemetryRecordedEvent` → alerting + analytics; `AlertIncidentChangedEvent` → notifications; `UserRegisteredEvent` → billing. Consequence: a slow OneSignal call in `AlertIncidentChangedEventListener` extends the telemetry ingestion transaction; an exception in `UserRegisteredEventHandler` rolls back user registration. Listeners wrap in `try/catch` to prevent the latter, which hides failures. Target: `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async` for side-effects, or the outbox. | `alerting/…/AlertingTelemetryRecordedEventListener`, `analytics/…/TelemetryAnalyticEventListener`, `notifications/…/AlertIncidentChangedEventListener`, `billing/…/UserRegisteredEventHandler` | MAJOR |
| H8 | Publishers register `afterCommit` synchronizations correctly (`AlertIncidentsChangedPublisher`, `DeviceCommandsPendingPublisher`, `ProvisioningDevicesChangedPublisher`). But `TelemetryEvaluationCommandServiceImpl.handle` publishes `TelemetryRecordedEvent` **before** commit (line 300), so alerting evaluates and may `alertRepository.save` inside the telemetry transaction. If alert creation throws (caught by the listener) nothing rolls back; if telemetry commit later fails, the alert was already published to the edge via `afterCommit`… of the *outer* transaction, which never commits. Causal chain stated; not observed at runtime. | `evaluation/application/internal/commandservices/TelemetryEvaluationCommandServiceImpl.java:298-300` | MAJOR |
| H9 | `DeviceSeedOnStartup` runs `SeedDevicesCommand(5)` on every boot in every environment, including production. Idempotent by serial number, but it also fires 5 edge notifications on first boot. | `device/application/internal/eventhandlers/DeviceSeedOnStartup.java` | MINOR |
| H10 | `DeviceSecretColumnDropMigration` executes `ALTER TABLE devices DROP COLUMN IF EXISTS device_secret` in `@PostConstruct` on every boot. Schema migration as a bean, alongside `ddl-auto: update`. Target: Flyway/Liquibase; delete this class once the migration has run everywhere. | `device/infrastructure/config/DeviceSecretColumnDropMigration.java` | MAJOR |

---

## I. Naming and placement (MINOR)

| # | Observed | Target (§2/§3) |
|---|---|---|
| I1 | Inbound ports under `<ctx>/domain/services/*CommandService`, `*QueryService` (all 7 contexts) | `<ctx>/application/commandservices/`, `<ctx>/application/queryservices/`. This is the only *systematic* placement deviation and is a pure move. |
| I2 | Aggregates under `domain/model/entities/` in alerting, analytics, device, evaluation, iam, notifications | `domain/model/aggregates/` for roots (`Alert`, `Device`, `Organization`, `Space`, `TelemetryEvaluation`, `User`, `DeviceDailySummary`…); `entities/` only for `DeviceCommand`, `DeviceAssignment` if they stay owned by `Device` (they have independent repositories today, so they are de facto aggregates — decide in branch 2). |
| I3 | Controllers under `interfaces/rest/controllers/` | `interfaces/rest/` directly. Harmless; keep if the team prefers. |
| I4 | Controller names singular (`AlertController`, `DeviceController`, `SpaceController`…) | Plural (`AlertsController`, `DevicesController`). |
| I5 | REST DTOs named `*Request` / `*Response` (`CreateSpaceRequest`, `DeviceResponse`) | `Create<Noun>Resource` / `<Noun>Resource`. iam already uses `*Resource` for responses. |
| I6 | Transforms named `*Transform` (analytics, evaluation) or absent (static `from()` on resources in device/alerting) | `<Noun>ResourceFromEntityAssembler`, `<Cmd>CommandFromResourceAssembler`. iam already conforms. |
| I7 | Spring Data repos named `<Aggregate>Repository` | `<Aggregate>PersistenceRepository` (the plain name goes to the domain port). |
| I8 | `application/internal/inboundservices/acl/` (alerting, analytics, device, evaluation, notifications) | Not in §2. Contents are event listeners → `application/internal/eventhandlers/`; integration event records → `interfaces/events/`. |
| I9 | `application/internal/outboundservices/acl/*IntegrationEvent` records and `*Publisher` classes | Event records → `interfaces/events/`; publishers are fine in `outboundservices` but should depend on a port, not `EdgeEventPublisher`. |
| I10 | `analytics/application/internal/services/` (6 files) | Not in §2. `KpiLiveMetricsBuffer`/`KpiLiveMetricsCache` are a domain concept (see 03-domain-improvements-analytics.md); schedulers → `application/internal/schedulers/` or command services with commands. |
| I11 | `iam/application/internal/commandservices/GoogleOAuthCallbackApplicationService` | Not a `*CommandServiceImpl`; either becomes one with a command or moves to `outboundservices/oauth`. |
| I12 | `iam/application/internal/outboundservices/acl/AsyncNotificationService` | Not an ACL; it's an `@Async` wrapper. `outboundservices/notifications/`. |
| I13 | `billing/domain/gateways/PaymentGateway` | Outbound port. Fine in domain per some schools; §2 puts outbound ports at `application/internal/outboundservices/payments/PaymentGateway`. |
| I14 | `billing/interfaces/web/StaticWebController` (Thymeleaf checkout demo) | Not in §2; demo code. Remove from production build or move to a `demo` profile. |
| I15 | `device/infrastructure/config/DevicePresenceSecurityConfiguration` | Security config belongs in `iam/infrastructure/authorization/` or `shared/infrastructure/security/`. See J3. |
| I16 | `alerting/domain/model/valueobjects/DailyAlertCount` | It's a read-model projection, not a VO used by the aggregate. `application/internal/queryservices/` or `interfaces/rest/resources`. |
| I17 | `analytics/domain/model/valueobjects/{KpiDashboardMetrics,OverviewDashboardSnapshot,AggregatedMetrics,DeviceMetricsSnapshot,KpiTrendPoint}` | Read models, not VOs of an aggregate. Acceptable in `domain/model/valueobjects` for a CQRS read side, but consider `domain/model/readmodels/`. |
| I18 | Query record `GetDashboardMetricsQuery.period` is a `String` compared with `equalsIgnoreCase("LIVE")` while `GetHistoricalTrendQuery.period` is the `TrendPeriod` enum. | Use `TrendPeriod` (add `LIVE`) in both. |
| I19 | Two controllers for one concept: `DeviceEdgeController` (`/api/v1/devices/presence/events`) and `EdgePresenceController` (`/api/v1/edge/presence`) both handle presence. | Keep one (the `/api/v1/edge/*` one, which is token-protected). |

---

## J. Build and configuration

Read: `pom.xml`, `src/main/resources/application.yml`, `.env.template`, `Dockerfile`, `.gitignore`.

| # | Finding | Severity |
|---|---|---|
| J1 | `maven-compiler-plugin` is **not declared** (inherited once from `spring-boot-starter-parent:3.5.5`). No duplicate. ✅ | INFO |
| J2 | Lombok is not a dependency; `<annotationProcessorPaths>` is not needed. ✅ | INFO |
| J3 | **Unauthenticated telemetry ingestion.** `POST /api/v1/evaluations/telemetry` is `permitAll` (`iam/infrastructure/config/SecurityConfiguration.java:80`) and is **not** in `ServiceTokenAuthenticationFilter.PATHS` (`shared/infrastructure/security/ServiceTokenAuthenticationFilter.java` covers only `/api/v1/edge/**` and `/api/v1/evaluations/telemetry/batch`). The controller performs no API-key or token check (`TelemetryEvaluationController.evaluateTelemetry`). Anyone who knows or guesses a `hardwareId` (`CLAIR-XXXXXX`) can inject readings, trigger alerts, and send push notifications to the owner. `Device.apiKey` exists and is shipped to the edge in the roster but is never verified on ingestion. | **BLOCKER** (security) |
| J4 | **Unauthenticated presence updates.** `DevicePresenceSecurityConfiguration` (`@Order(0)`, `securityMatcher("/api/v1/devices/presence/events")`, `permitAll`) creates a separate filter chain with **no** token filter. Anyone can mark any device ONLINE/OFFLINE/ERROR. | **BLOCKER** (security) |
| J5 | `DeviceRosterController` re-implements the token check inline (`MessageDigest.isEqual`) although `ServiceTokenAuthenticationFilter` already covers `/api/v1/edge/**`. Duplicate security logic; the legacy `X-Edge-Token` header fallback exists in both places. | MINOR |
| J6 | `/api/v1/devices/provisioning` and `/api/v1/devices/commands/pending` are `permitAll` in `SecurityConfiguration` but no controller maps those paths any more (grep finds none). Dead allow-rules; remove. | MINOR |
| J7 | `spring.jpa.hibernate.ddl-auto: update` in the only profile. README says "switch to `validate` once it boots". No migration tool. Combined with H10, schema is managed by two ad-hoc mechanisms. | MAJOR |
| J8 | Secrets: none hard-coded in `application.yml` (all `${VAR}`). `.env` is git-ignored ✅ and not tracked ✅. `.env.template` contains placeholder values only (`sk_test_1234…`, `change-me-…`) ✅. Redis default `password: ${REDIS_PASSWORD:admin}` is a weak default that applies if the env var is unset. | MINOR |
| J9 | Physical naming strategy not declared; Spring Boot default applies (see G). Cannot be confirmed against DDL without a database; **branch 2 must capture `hibernate.hbm2ddl` output before and after the split and diff it.** | INFO |
| J10 | No test profile config (`src/test/resources` has only Cucumber features). Tests that boot Spring will hit `${DB_URL}` unresolved. H2 is on the classpath (`pom.xml:182`) but unused. | MINOR |
| J11 | `spring-boot-devtools` is a compile-scope dependency without `<optional>true</optional>` or a profile guard (`pom.xml:132`). Ships in the production jar. | MINOR |
| J12 | `EdgeEventPublisher` builds `new RestTemplate()` with no connect/read timeout. | MAJOR (also in G) |

---

## K. UNCLASSIFIED files

Files matching no role in §2. Listed, not guessed.

| # | Path | Note |
|---|---|---|
| K1 | `shared/infrastructure/edge/EdgeEventPublisher.java` | Outbound HTTP adapter without a port. |
| K2 | `shared/infrastructure/persistence/jpa/outbox/OutboxMessage.java` | Dead (0 consumers). |
| K3 | `shared/infrastructure/persistence/jpa/outbox/OutboxMessageRepository.java` | Dead. |
| K4 | `device/infrastructure/config/DeviceSecretColumnDropMigration.java` | Runtime schema migration. |
| K5 | `device/infrastructure/config/DevicePresenceSecurityConfiguration.java` | Second security chain. |
| K6 | `device/application/internal/eventhandlers/DeviceSeedOnStartup.java` | Startup seeder; not an event handler in the DDD sense. |
| K7 | `analytics/application/internal/services/AnalyticsSseService.java` | SSE emitter registry + `@EventListener`; part infrastructure (web), part application. |
| K8 | `analytics/application/internal/services/KpiLiveMetricsBuffer.java` | In-memory sliding window; domain logic in application layer. |
| K9 | `analytics/application/internal/services/KpiLiveMetricsCache.java` | Caffeine cache of K8; infrastructure. |
| K10 | `analytics/application/internal/services/SnapshotAggregationScheduler.java` | Scheduler + write logic + cross-context raw rows. |
| K11 | `analytics/application/internal/services/DailyReportAggregationService.java` | Scheduler + `JdbcTemplate` + accumulator. |
| K12 | `analytics/application/internal/services/MonthlyReportAggregationService.java` | Scheduler + accumulator. |
| K13 | `billing/interfaces/web/StaticWebController.java` | Thymeleaf demo page. |
| K14 | `iam/infrastructure/oauth/google/GoogleOAuthStateManager.java` | Injected directly by a controller (B2-3); role unclear (state store for OAuth CSRF?). |

Also dead-code inventory (0 consumers, delete in branch 12): `alerting/domain/model/events/{AlertCreatedEvent,AlertResolvedEvent}`, `device/domain/model/events/DeviceRegisteredEvent`, `{alerting,analytics,evaluation}/application/internal/inboundservices/acl/TelemetryRecordedIntegrationEvent`, `EdgeEventPublisher.publish{AlertIncident,DeviceCommand,DeviceChanged}` (`@Deprecated`, unused), `TelemetryEvaluationRepository.findLatestByDeviceId` (unused; duplicate of `findByDeviceId`).

---

## L. Reference deviations observed (INFO)

| # | §7 item | Observed here | Verdict |
|---|---|---|---|
| L1 | Setters on aggregate for the assembler | **Not present** — aggregates have no setters; `User.rehydrate(...)` is a static reconstruction factory. | Better than the reference. Branch 5 must use the factory pattern for every aggregate and must **not** introduce setters. |
| L2 | `FetchType.EAGER` on collections | Not present; all associations LAZY. | Better than the reference, at the cost of relying on open-in-view (H6). |
| L3 | `Result`/`ApplicationError` mixed with exceptions | Not present; exceptions only, mapped in `GlobalExceptionHandler`. One `Outcome` enum in the edge ack services (H5), justified. | Consistent. Keep exceptions; do not introduce `Result`. |
| L4 | `AbstractAggregateRoot` for events | Used directly by two billing aggregates only; every other aggregate publishes events from the application service via `ApplicationEventPublisher`. | Pick one. Recommendation: introduce `shared/domain/model/aggregates/AbstractDomainAggregateRoot`, have aggregates `registerEvent(...)`, and let Spring Data publish on `save()`. That also fixes H8 (events published pre-commit) because Spring Data publishes after the repository call inside the same transaction and listeners can use `@TransactionalEventListener`. |
| L5 | Java version | `java.version` 25 on Spring Boot 3.5.5 with `maven-enforcer` requiring `[25,)`. Spring Boot 3.5 officially supports up to Java 24 per its system requirements at release; 25 works because Hibernate 6.6/Jackson 2.19 run on it, but any bytecode-level library (Mockito inline, ByteBuddy) may need `-Dnet.bytebuddy.experimental=true`. | Verify `mvn test` passes on the CI JDK before branch 14 adds ArchUnit (which uses ASM). |

---

## Step 11 — Architecture test coverage

No ArchUnit dependency in `pom.xml`; no test references `ArchRule`. **Planned deliverable, branch 14.** The rules to encode are exactly: §4.1 (no `jakarta.persistence`, `o.s.web`, `o.s.data.jpa`, `o.s.stereotype` under `..domain..`), the §4 table (layer edges), and "cross-context imports only target `..interfaces.acl..` or `..interfaces.events..`".
