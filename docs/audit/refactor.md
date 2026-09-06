# clair-core — Structural Refactor Plan by Bounded Context

Baseline: branch `refactor/domain-purity` at `04f3c95` (zero source changes ahead of `main`). Reference: `upc-pre-202610-1asi0729-11848/learning-center-platform` (verified by clone and grep on 2026-09-05).

Scope of this document: packaging and persistence structure only. No behaviour change. Anything in the former `03-` and `04-domain-improvements` documents is out of scope and goes to a separate backlog.

## The one finding

The reference has zero `jakarta.persistence` imports under any `domain/` package. clair-core has 59 of 188 domain files importing JPA, Spring or Hibernate, because **every aggregate is also its own JPA entity** (`Device` is `@Entity @Table`, `AuditableModel` is a `@MappedSuperclass` living in `shared/domain`). Only `notifications` has a `domain/repositories` folder. Everything else the old audit listed (services injecting Spring Data interfaces, controllers touching repositories, `Page`/`Pageable` in ports) is a consequence of that single conflation and disappears when it is fixed.

The fix is not "remove the imports". Removing `@Entity` from an aggregate breaks persistence until four other files exist. The fix is: **for each aggregate, create the reference's five infrastructure files, then strip the aggregate.** Done per context, smallest first.

## Target shape per aggregate `X` (reference, copied exactly)

| Role | Path under `<ctx>/` | Kind |
|---|---|---|
| Aggregate | `domain/model/aggregates/X` | plain class, extends `shared.domain.model.aggregates.AbstractDomainAggregateRoot`, no framework imports |
| Value objects | `domain/model/valueobjects/*` | records, self-validating in compact constructor, no annotations |
| Port | `domain/repositories/XRepository` | interface, domain types only: `Optional<X> findById(UUID)`, `X save(X)`, `boolean existsBy…(VO)` |
| Inbound ports | `application/commandservices/XCommandService`, `application/queryservices/XQueryService` | interfaces |
| Inbound impls | `application/internal/commandservices/XCommandServiceImpl`, `application/internal/queryservices/XQueryServiceImpl` | `@Service`, `@Transactional` on command impls only, inject the port |
| Persistence entity | `infrastructure/persistence/jpa/entities/XPersistenceEntity` | `@Entity`, extends `shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity`, carries every annotation the aggregate has today, byte-identical column definitions |
| VO mapping | `infrastructure/persistence/jpa/converters/<VO>PersistenceConverter` (single field) or `…/embeddables/<VO>PersistenceEmbeddable` (multi field) | `@Converter(autoApply = true)` / `@Embeddable` |
| Spring Data | `infrastructure/persistence/jpa/repositories/XPersistenceRepository` | `extends JpaRepository<XPersistenceEntity, UUID>`; existing `@Query` bodies kept, entity name swapped |
| Assembler | `infrastructure/persistence/jpa/assemblers/XPersistenceAssembler` | `final`, private constructor, exactly two static methods `toDomainFromPersistence` and `toPersistenceFromDomain`, null-safe, delegates for nested types |
| Adapter | `infrastructure/persistence/jpa/adapters/XRepositoryImpl` | `@Repository`, implements the port, the **only** class naming both `X` and `XPersistenceEntity` |
| REST | `interfaces/rest/XsController`, `interfaces/rest/resources/*Resource`, `interfaces/rest/transform/XResourceFromEntityAssembler`, `<Cmd>CommandFromResourceAssembler` | plural controller, records for resources |
| ACL | `interfaces/acl/<Ctx>ContextFacade` (+ DTO records), `application/acl/<Ctx>ContextFacadeImpl` | facade exposes DTOs, never domain types |
| Integration events | `interfaces/events/<Event>IntegrationEvent` | record; the only event type another context may import |

### Application layer layout (reference, exact)

| Reference `application/` | clair today | Rule |
|---|---|---|
| `commandservices/`, `queryservices/` — inbound ports | `domain/services/` | move |
| `internal/commandservices/`, `internal/queryservices/` — impls | same | keep |
| `internal/eventhandlers/` — listeners for own and integration events | `internal/inboundservices/acl/*Listener` | rename; the reference has no `inboundservices`; inbound is REST or an event handler, nothing else |
| `internal/outboundservices/acl/External*Service` — calls another context's facade | same | keep; may import only `<other>.interfaces.acl` and `<other>.interfaces.events` |
| `internal/outboundservices/<capability>/` — ports for infrastructure capabilities (hashing, tokens, oauth, email, push, payments, edge) | concrete infrastructure classes injected directly | port here, implementation stays in `infrastructure/` |
| `acl/<Ctx>ContextFacadeImpl` — implements `interfaces/acl/<Ctx>ContextFacade` | same | keep; delegates to own query services, never to repositories or `JdbcTemplate` |
| not in reference | `internal/services/`, `internal/outboundservices/acl/*IntegrationEvent`, `*Publisher` | dissolve: schedulers → `internal/schedulers/`, integration event records → `interfaces/events/`, publishers → `outboundservices/edge/` behind an `EdgeNotifier` port |

## Application and interface sub-packages (reference, verified)

| Concern | Path under `<ctx>/` | Kind and contract |
|---|---|---|
| Inbound port, writes | `application/commandservices/XCommandService` | interface; one `handle(<Cmd>Command)` per command |
| Inbound port, reads | `application/queryservices/XQueryService` | interface; one `handle(<Q>Query)` per query |
| Inbound impls | `application/internal/commandservices/XCommandServiceImpl`, `application/internal/queryservices/XQueryServiceImpl` | `@Service`; inject `domain/repositories` ports and outbound services only |
| Event handlers | `application/internal/eventhandlers/<Event>EventHandler` | `@Service`, `@EventListener` method `on(<Event>IntegrationEvent)`; builds a command and calls the own context's command service. Imports from another context only `<other>.interfaces.events.*` |
| Outbound ACL | `application/internal/outboundservices/acl/External<Other>Service` | concrete `@Service`; wraps `<other>.interfaces.acl.<Other>ContextFacade`; converts primitives to the caller's own VOs (`Optional<ProfileId>`); the **only** class in a context that imports another context's facade |
| Outbound technical ports | `application/internal/outboundservices/<tech>/XService` | interface (`HashingService`, `TokenService`); implementation in `infrastructure/<tech>/XServiceImpl` |
| Published contract | `interfaces/acl/<Ctx>ContextFacade` | interface; parameters and returns are primitives or DTO records, never domain types |
| Facade impl | `application/acl/<Ctx>ContextFacadeImpl` | `@Service`; injects the own command/query services; never a repository, never `@Transactional` |
| Integration events | `interfaces/events/<Event>IntegrationEvent` | record with a static `from(<Aggregate>)`; published by the owning context; the only event another context may listen to |

Not in the reference, therefore not in clair after the refactor: `application/internal/inboundservices/**`, `application/internal/services/**`, `domain/services/*CommandService|*QueryService`, `*Publisher` classes under `outboundservices/acl`, integration-event records under `application/**`.

### clair folder mapping (applies in every phase)

| clair today | target |
|---|---|
| `domain/services/*CommandService`, `*QueryService` | `application/commandservices/`, `application/queryservices/` |
| `domain/services/*DomainService` + `Impl` (analytics only) | stay; are real domain services; lose `@Service`, registered by `@Bean` |
| `domain/services/<outbound>` (`EmailDeliveryService`, `PushNotificationDeliveryService`, `GoogleTokenVerifier`), `domain/gateways/PaymentGateway` | `application/internal/outboundservices/<tech>/` |
| `application/internal/inboundservices/acl/*EventListener` | `application/internal/eventhandlers/*EventHandler` |
| `application/internal/inboundservices/acl/*IntegrationEvent` (three unused copies) | delete; consume the owning context's `interfaces/events/*IntegrationEvent` |
| `application/internal/outboundservices/acl/External*Service` | correct; keep |
| `application/internal/outboundservices/acl/*IntegrationEvent` | `interfaces/events/` |
| `application/internal/outboundservices/acl/*Publisher` | `application/internal/outboundservices/edge/`, depending on port `shared/application/outboundservices/EdgeNotifier` |
| `application/internal/services/*` (analytics) | command services + `application/internal/schedulers/`, or `infrastructure/` for cache and SSE |
| `application/acl/*ContextFacadeImpl` injecting repositories or `JdbcTemplate` | inject own query services only |
| `interfaces/acl/*ContextFacade` exposing domain VOs or enums | primitives or DTO records in `interfaces/acl/` (`OrganizationSummary`, `SpaceSummary` are already correct) |
| `interfaces/rest/controllers/X` | `interfaces/rest/XsController` |
| `*Request` / `*Response` | `Create<X>Resource` / `<X>Resource` |
| `*Transform`, static `from()` on resources | `interfaces/rest/transform/<X>ResourceFromEntityAssembler`, `<Cmd>CommandFromResourceAssembler` |
| Spring Data `<X>Repository` in `infrastructure/persistence/jpa/repositories` | `<X>PersistenceRepository` there; `<X>Repository` becomes the port in `domain/repositories`; `<X>RepositoryImpl` in `infrastructure/persistence/jpa/adapters` |

Reconstruction: aggregates gain a static factory `reconstitute(...)` taking every field including id and timestamps (the existing `User.rehydrate` pattern). **No setters** for the assembler; the reference's `setId` is a known deviation, not a model.

## Control gate, every phase

Before touching a context, boot with `spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create` writing to a file, commit that file. After the phase, regenerate and `diff`. Empty diff (modulo explicitly listed removals) is the definition of "no behaviour change" for persistence. This requires a running Postgres; H2 output differs in dialect and is not a valid control.

Second gate: the existing test suite green after the mechanical rename of mocked repository types to the new ports.

**What the gate cannot see.** Identical DDL does not mean identical runtime behaviour. Two changes
this refactor makes are invisible to it and must be checked by hand in the phase that makes them:
who assigns the primary key (`@GeneratedValue` vs. assigned, which flips `save()` between `persist`
and `merge`), and who populates `created_at` / `updated_at` (see the auditing note in Phase 1).

---

## Phase 0 — Shared kernel

**Create**
- `shared/domain/model/aggregates/AbstractDomainAggregateRoot` — ~~wraps `org.springframework.data.domain.AbstractAggregateRoot` (the single documented framework exception in domain)~~ **Rewritten in Phase 3:** it holds its own event list and imports nothing from any framework, because the Spring class only publishes the events of the instance a repository is handed — the persistence entity after the split, never the aggregate. Exposes `registerEvent` to subclasses and `domainEvents()` / `clearDomainEvents()` to the adapter that drains them. `PaymentRecord` extends it.
- `shared/infrastructure/persistence/jpa/entities/AuditableAbstractPersistenceEntity` — `@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)`, fields `id` (UUID), `createdAt`, `updatedAt` with `@CreatedDate`/`@LastModifiedDate`. **Keep `java.util.Date` for now**; switching to `Instant` changes the generated column type and breaks the DDL gate. Change it in Phase 8.
- `shared/domain/model/PageResult<T>` — record `(List<T> items, int page, int size, long total)`. Replaces `org.springframework.data.domain.Page` in every port and query record.
- `shared/domain/exceptions/ResourceNotFoundException` — so `GlobalExceptionHandler` stops importing `analytics.domain.exceptions`.

**Delete / move**
- `shared/domain/model/entities/AuditableModel` is deleted at the end of Phase 7 when its last subclass is gone. Until then it stays; do not move it (a domain class pointing at infrastructure is a worse intermediate state than the current one).
- `shared/infrastructure/persistence/jpa/outbox/*` — 0 consumers. Delete. Record `outbox_message` as the one permitted line in the DDL diff.
- `GlobalExceptionHandler` → `shared/interfaces/rest/GlobalExceptionHandler`, returns `ErrorResource` record.

**Done when** the app boots, all tests pass, DDL diff shows only `outbox_message` removed.

**Carried forward from Phase 0** — two decisions are made by the shared kernel but only bite when
the first context adopts it, in Phase 1:

- `AuditableAbstractPersistenceEntity` declares `@Id` with **no generator**. Every persistence
  entity built from here on gets its id from the aggregate, and `SimpleJpaRepository.save()` will
  therefore see a non-null id and call `merge` instead of `persist`. See the identity note in
  Phase 1 for the `Persistable<UUID>` flag that keeps it a plain insert.
- It also declares `@CreatedDate` / `@LastModifiedDate` **on the entity itself**, which is not how
  the timestamps are filled today. See the auditing note in Phase 1.
- The timestamps are `Instant`, not the `Date` this phase originally wrote, and
  `@JdbcTypeCode(SqlTypes.TIMESTAMP)` pins them to a plain `timestamp(6)` column. Without it
  Hibernate maps `Instant` to `timestamp with time zone` and every table a phase touches changes
  type — measured, not assumed. Phase 8 deletes the two annotations and migrates every table at
  once.

---

## Phase 1 — notifications

Smallest. Two aggregates, no other context reads its tables, already has a partial port.

| Today | Target |
|---|---|
| `domain/model/entities/EmailLog` (`@Entity`, table `email_logs`, `@UuidGenerator`) | `domain/model/aggregates/EmailLog` plain; `EmailLogPersistenceEntity` |
| `domain/model/entities/PushNotificationLog` (table `push_notification_logs`) | `domain/model/aggregates/PushNotificationLog`; `PushNotificationLogPersistenceEntity` |
| VOs `EmailRecipient`, `EmailSubject`, `EmailContent` (`@Embeddable`, `@Column`) | records without annotations; three `*PersistenceConverter` (all single-field) |
| `domain/repositories/EmailLogPersistence` (save only), `PushNotificationHistoryRepository` (leaks `Page`) | `EmailLogRepository`, `PushNotificationLogRepository` ports with `PageResult` |
| Spring Data `EmailLogRepository`, `PushNotificationLogRepository` **extending the port** | `EmailLogPersistenceRepository`, `PushNotificationLogPersistenceRepository` extending only `JpaRepository`; the port is implemented by the adapter, never extended |
| `domain/services/EmailCommandService`, `PushNotificationHistoryQueryService` | `application/commandservices/`, `application/queryservices/` |
| `domain/services/EmailDeliveryService`, `PushNotificationDeliveryService` (outbound ports) | `application/internal/outboundservices/{email,push}/` — the reference puts outbound ports in application, not domain |
| `application/internal/inboundservices/acl/AlertIncidentChangedEventListener` importing `alerting.domain.model.events.*` | `application/internal/eventhandlers/…` consuming `alerting.interfaces.events.AlertIncidentChangedIntegrationEvent` (created in Phase 4; until then keep the old import and record it) |
| `NotificationController` | `NotificationsController`; `PushNotificationResponse` → `PushNotificationResource` + `PushNotificationResourceFromEntityAssembler` |

Identity: `@UuidGenerator` today means the database assigns. After the split the aggregate assigns `UUID.randomUUID()` in its constructor and the persistence entity has no generator. DDL unaffected — but `SimpleJpaRepository.isNew()` is `id == null`, so with an assigned id every `save()` becomes a `merge`: one extra `SELECT` per insert, and the returned instance is a different object from the one passed in. `AuditableAbstractPersistenceEntity` therefore implements `Persistable<UUID>`, done here, in the first context that adopts the base class, rather than once per phase. `isNew()` reads `createdAt == null` instead of a `@Transient` flag: a persistence assembler builds a fresh entity on every save, so a flag defaulting to `true` would claim that a previously stored aggregate is new, while `createdAt` is filled by `@CreatedDate` on the first insert and travels back through the assembler on every later one.

Auditing: ~~`AuditingEntityListener` does not descend into embeddables, so the contexts whose audit fields live in an `@Embeddable` inner class are not audited today and will start being audited when they move onto `AuditableAbstractPersistenceEntity`.~~ **Wrong, corrected in Phase 2.** Persisting a `TelemetryEvaluation`, whose audit fields sit in the embedded `EvaluationAudit`, fills `created_at` and `updated_at` exactly as the flat subclasses are filled: Spring Data's auditing walks into embedded properties. The `ReflectionTestUtils` in `DeviceRepositoryTest` pins a specific timestamp for a cursor assertion, and `Device.touchUpdatedAt()` forces the watermark to move on changes Hibernate would not consider dirty — neither compensates for missing auditing. Moving a context onto the base class therefore changes nothing here, in either direction. `touchUpdatedAt` still has to be decided in Phase 7, on its own merits.

**Done when** `grep -rE 'jakarta.persistence|org.springframework' notifications/domain` is empty; DDL diff empty; tests green.

**Done, 2026-09-06.** All three gates met: the grep is empty, `schema-phase-1.sql` is byte-identical to `schema-phase-0.sql`, 478 tests pass. Deviations from the table above, all deliberate:

- The event handler may not touch a repository, so the push path grew the inbound port the reference asks for: `application/commandservices/PushNotificationCommandService` + `SendPushNotificationCommand`, with the delivery-and-log `try`/`catch` moved out of the handler into `PushNotificationCommandServiceImpl`. The handler now only resolves owner and device name and issues the command.
- `Page<EmailLog> findByRecipientEmail(EmailRecipient, Pageable)` had no callers at all and is gone. `findByRecipientEmail(EmailRecipient)` survives as the port method `findByRecipient`, used by the adapter test.
- The port speaks `PageResult`, but `NotificationsController` still answers with a Spring Data `Page` envelope, rebuilt from the `PageResult`. The response body is a published contract; changing `content`/`totalElements` to `items`/`total` is an API change, not a refactor. The sort the controller used to pass down (`createdAt` descending) now lives in the adapter, which is the only layer that should know a column name.
- Aggregates are plain classes and do **not** extend `AbstractDomainAggregateRoot`: neither publishes events, and see the note in Phase 3 for why the base class does nothing on a domain aggregate once the split is done.
- Still crossing a boundary, both pre-existing and both scheduled: the event handler imports `alerting.domain.model.events` (Phase 4), and `NotificationsController` imports `iam.infrastructure.tokens.jwt.JwtAuthenticationFilter` for its `USER_ID_ATTRIBUTE` constant (Phase 6).

---

## Phase 2 — evaluation

One aggregate, but its **column names are read by raw SQL in analytics** (`DailyReportAggregationService`, `EvaluationContextFacadeImpl`, table `telemetry_evaluations`). The DDL gate matters most here.

| Today | Target |
|---|---|
| `domain/model/entities/TelemetryEvaluation` with five embedded VOs via `@AttributeOverride` | `domain/model/aggregates/TelemetryEvaluation`; `TelemetryEvaluationPersistenceEntity` carrying every `@AttributeOverride` verbatim |
| VOs `Location`, `Connectivity`, `ParticulateMatter`, `AirQuality` (multi-field), `DeviceId` (single) | four `*PersistenceEmbeddable`, one `DeviceIdPersistenceConverter` |
| Spring Data `TelemetryEvaluationRepository` | port `TelemetryEvaluationRepository` + `TelemetryEvaluationPersistenceRepository` + `TelemetryEvaluationRepositoryImpl` |
| `EvaluationContextFacadeImpl.getHourlyTelemetryAggregation` returning `List<Map<String,Object>>` via `JdbcTemplate` | adapter method on the port returning `List<HourlyDeviceAverage>` record; facade delegates to the query service |
| `domain/services/*Service` | `application/*services/` |
| `domain/model/events/TelemetryRecordedEvent` consumed by alerting and analytics directly | keep as internal domain event; **add** `interfaces/events/TelemetryRecordedIntegrationEvent` record; downstream contexts switch in Phases 3 and 5 |
| `TelemetryEvaluationTransform` | `TelemetryEvaluationResourceFromEntityAssembler` |
| Query records already carry `page`/`size` ints | keep; this is the model for the other contexts |

**Done when** DDL diff empty **and** analytics' two raw-SQL readers still return the same rows on a seeded database.

**Done, 2026-09-06.** Gates met: `grep -rE 'jakarta.persistence|org.springframework' evaluation/domain` is empty, `schema-phase-2.sql` is byte-identical to `schema-phase-1.sql`, 487 tests pass. `TelemetryEvaluationRepositoryImplTest` seeds through the port and then runs both raw-SQL readers against the result, including the exact column list `DailyReportAggregationService` selects. Deviations:

- The facade no longer holds a `JdbcTemplate`. The hourly aggregation moved to `TelemetryEvaluationRepositoryImpl` as `findHourlyAveragesBetween`, SQL unchanged, and the facade now returns `List<HourlyTelemetryAverage>` records instead of `List<Map<String,Object>>`. That is a published-contract change, so analytics' `ExternalEvaluationService` and `SnapshotAggregationScheduler` moved with it; the cast-juggling those had over the raw maps is gone.
- `interfaces/events/TelemetryRecordedIntegrationEvent` exists and **is published**, alongside the internal `TelemetryRecordedEvent` that alerting and analytics still consume. Publishing both now means the switch in the later phases is one line on each side. Stop publishing the internal event when the last consumer moves.
- Dropped `findLatestByDeviceId(UUID, Pageable)` returning a list: no callers.
- The two JPQL `@Query` bodies are gone. With `DeviceId` behind a converter, `te.deviceId.value` is no longer a path; derived queries (`findByDeviceIdOrderByRecordedAtDesc`) express the same thing and keep the ordering.
- Watch out in the later contexts: the aggregation binds a `java.sql.Timestamp`, so the window bounds are read in the database session's time zone. Pre-existing and unchanged, but it makes any window measured in minutes environment-dependent, which is why the adapter test uses a day of slack.

---

## Phase 3 — billing

**Before anything else, settle domain events.** `UserPlan` and `PaymentRecord` extend
`AbstractDomainAggregateRoot` and register events today, and Spring Data publishes those only for
the instance handed to a repository's `save()`. After the split that instance is the *persistence
entity*, so events registered on the aggregate are silently dropped. Either the adapter drains
`domainEvents()` and publishes them itself after a successful save, or the persistence entity
carries the events. Phase 1 did not hit this — notifications publishes none — and no test would
have caught it.

| Today | Target |
|---|---|
| `domain/model/aggregates/UserPlan`, `PaymentRecord` (`@Entity`, extend Spring `AbstractAggregateRoot`, no explicit `@Table` → `user_plan`, `payment_record`) | extend `AbstractDomainAggregateRoot`; `UserPlanPersistenceEntity`, `PaymentRecordPersistenceEntity` with explicit `@Table(name = "user_plan")` / `"payment_record"` so the gate holds |
| VOs `Money` (multi), `UserId` (single) | `MoneyPersistenceEmbeddable`, `UserIdPersistenceConverter` |
| `SubscriptionPaidEvent extends ApplicationEvent` | plain record; publisher passes it to `publishEvent(Object)` |
| `UserRegisteredEventHandler` importing `iam.domain.model.events.UserRegisteredEvent` | consume `iam.interfaces.events.UserRegisteredIntegrationEvent` (Phase 6); until then record the import |
| `domain/gateways/PaymentGateway` | `application/internal/outboundservices/payments/PaymentGateway`; `StripePaymentGatewayAdapter` unchanged |
| `interfaces/web/StaticWebController` (Thymeleaf demo) | behind a `demo` profile or deleted |

**Done when** `grep -rE 'jakarta.persistence|org.springframework' billing/domain` is empty; DDL diff
empty; tests green.

**Done, 2026-09-06.** All three gates met: the grep is empty, `schema-phase-3.sql` is byte-identical
to `schema-phase-2.sql`, 493 tests pass. Deviations:

- **Domain events, settled: the adapter drains.** `PaymentRecordRepositoryImpl.save()` publishes
  `paymentRecord.domainEvents()` after the write and then clears them. The alternative — the
  persistence entity carrying the events — was rejected because it puts a domain concern in a
  storage type and only works while every write goes through a Spring Data `save()`.
- **`AbstractDomainAggregateRoot` no longer extends Spring Data's `AbstractAggregateRoot`.** It now
  holds its own event list with `registerEvent`, `domainEvents()` and `clearDomainEvents()`, and
  imports nothing from any framework. The old base class only worked by having the repository read
  the events off the instance it was handed, which after the split is the persistence entity — so
  the wrapper was not just an unnecessary dependency, it was a silent bug. This deletes the one
  framework import `domain` was allowed to keep; `AuditableModel` (Phase 7) is now the last.
- `UserPlan` does **not** extend the base class: it publishes nothing. Only `PaymentRecord` does.
- `SubscriptionPaidEvent` is a record `(String stripePaymentIntentId, UserId userId)`. The `source`
  argument `ApplicationEvent` demanded is gone; `@EventListener` binds the payload either way.
- `StaticWebController` is **deleted**, not profiled. `/checkout-demo` returned the Thymeleaf view
  `checkout`, and there is no `src/main/resources/templates` directory — the endpoint has been a 500
  for as long as the template has been missing. Its `permitAll()` entry in `SecurityConfiguration`
  went with it, and so did the only reader of the `stripe.public.key` property.
- Inbound ports follow Phases 1 and 2: `application/commandservices/SubscriptionCommandService` and
  `application/queryservices/SubscriptionQueryService`, impls under `application/internal/`.
- `PaymentRecordRepository` gained `findById`, which the query service used to reach through
  `JpaRepository` for. `PaymentIntentResult` stays in `domain/model/valueobjects`; it is the
  gateway's result shape and names nothing from Stripe.
- Both aggregates keep their audit timestamps as constructor state, `null` until first written, so
  `isNew()` still reads `createdAt == null` and an assigned id is still a plain insert.
- Still crossing a boundary, scheduled: `UserRegisteredEventHandler` imports
  `iam.domain.model.events.UserRegisteredEvent` (Phase 6).

---

## Phase 4 — alerting

| Today | Target |
|---|---|
| `domain/model/entities/Alert` | `domain/model/aggregates/Alert`; `AlertPersistenceEntity` |
| `AlertRepository` JPQL `JOIN Device` (cross-context database join) | the adapter calls `DeviceContextFacade.findHardwareIdsByDeviceIds(List<UUID>)` — add that batch method to the device facade **first**, in this phase, as the only touch on device |
| Query records with `Pageable`; `AlertQueryService` returning `Page` | `page`/`size` ints; `PageResult<Alert>` |
| `AlertCommandServiceImpl`, `ExternalAlertingThresholdService` importing `device.domain.model.valueobjects.DeviceMetricThresholdConfiguration` | device facade returns `ThresholdSummary(String metric, BigDecimal value, boolean enabled)` DTO; alerting maps to its own `MetricType`. Change the facade signature in this phase |
| `AlertingContextFacade.getRecentAlertsByOwnerId(UUID, List<AlertStatus>, int)` exposing a domain enum | takes `List<String>`; returns `AlertDetails` DTO (already exists) |
| `application/internal/outboundservices/acl/AlertIncidentChangedIntegrationEvent` | move to `interfaces/events/`; notifications switches to it |
| `AlertingTelemetryRecordedEventListener` importing evaluation's domain event | consume `TelemetryRecordedIntegrationEvent` from Phase 2 |
| `EdgeAlertController` injecting `AlertRepository`, hand-building `Map<String,Object>` | `GetPendingEdgeAlertsQuery` on the query service; `EdgeAlertResource` record |
| `EdgeAlertAcknowledgementService` taking REST DTO `EdgeAlertAckRequest` | `AcknowledgeEdgeAlertCommand` |
| `domain/model/valueobjects/DailyAlertCount` | it is a read projection → `application/internal/queryservices/` or a resource |
| `AlertController` importing `iam.infrastructure.tokens.jwt.JwtAuthenticationFilter` for a constant | `shared/interfaces/rest/security/CurrentUserId` argument resolver (create here; other controllers adopt it in their phases) |

---

## Phase 5 — analytics

Writers are schedulers, not command services. The move fixes the self-invocation defect (`@Scheduled aggregatePreviousDay()` calling `@Transactional generateForDate()` on `this`, so the proxy never intercepts) as a side effect; record that in the phase notes because it is a behaviour correction.

| Today | Target |
|---|---|
| `domain/model/entities/DeviceAnalyticsSnapshot`, `DeviceDailySummary`, `DeviceMonthlySummary` (tables `device_analytics_snapshots`, `device_daily_summaries`, `device_monthly_summaries`) | `domain/model/aggregates/`; three `*PersistenceEntity` |
| VOs `MetricStats`, `AqiCategoryBreakdown`, `AirQualityIndex`, `MetricTrend` (multi), `DeviceId` (single) | four embeddables, one converter |
| `domain/services/*DomainServiceImpl` with `@Service` | keep interfaces and impls in `domain/services` (they are real domain services), remove `@Service`, register via `infrastructure/config/AnalyticsDomainServicesConfiguration` `@Bean` methods |
| `domain/services/*QueryService`, `KpiLiveMetricsCommandService` | `application/*services/` |
| `application/internal/services/DailyReportAggregationService`, `MonthlyReportAggregationService`, `SnapshotAggregationScheduler` | `application/internal/commandservices/*CommandServiceImpl` behind `GenerateDailySummaryCommand(LocalDate)`, `GenerateMonthlySummaryCommand(YearMonth)`, `AggregateHourlySnapshotCommand(Instant)`; a thin `application/internal/schedulers/AnalyticsScheduler` holds `@Scheduled` and calls the ports |
| raw SQL against `telemetry_evaluations` via `JdbcTemplate` | call `EvaluationContextFacade` (Phase 2 adapter method) |
| `OverviewDashboardQueryServiceImpl` importing `alerting.domain.model.valueobjects.AlertStatus` | use the string-based facade from Phase 4 |
| `KpiLiveMetricsBuffer`, `KpiLiveMetricsCache`, `AnalyticsSseService` | `infrastructure/` (cache, SSE) — they are not application services |
| `DeviceTelemetryUnavailableException` | extends `shared.domain.exceptions.ResourceNotFoundException` |
| `*Transform` | `*ResourceFromEntityAssembler` |
| `GetDashboardMetricsQuery.period: String` | `TrendPeriod` enum with `LIVE` added |

---

## Phase 6 — iam

Two persistence technologies. The reference has no Redis; the pattern below is an extension, stated here so it is not invented ad hoc.

| Today | Target |
|---|---|
| `domain/model/entities/User` (`@Entity`, table `users`) | `domain/model/aggregates/User`; `UserPersistenceEntity`; keep `User.rehydrate` as the assembler's only reconstruction path |
| VOs `EmailAddress` (also `@Email`, Jackson), `Password`, `UserId`, `GoogleUserId`, `GoogleIdToken`, `TokenJti`, `VerificationCode`, `RegistrationSessionId` | records, no annotations; converters for the ones stored in `users` |
| `RegistrationSession`, `TokenSession` records with Jackson annotations, stored as JSON in Redis by concrete `*Repository` classes | ports `RegistrationSessionRepository`, `TokenSessionRepository` in `domain/repositories`; `infrastructure/persistence/redis/documents/<X>RedisDocument` carrying Jackson; `…/redis/assemblers/<X>RedisAssembler`; `…/redis/adapters/<X>RepositoryImpl`; current concrete classes become package-private |
| `UserRegisteredEvent extends ApplicationEvent` | plain record; **add** `interfaces/events/UserRegisteredIntegrationEvent` for billing |
| services injecting `JwtTokenEncoder`, `GoogleAuthorizationCodeTokenClient` (concrete infrastructure) | outbound ports `application/internal/outboundservices/tokens/TokenService`, `…/oauth/GoogleTokenExchange`; impls stay where they are |
| `AuthenticationController` importing `GoogleOAuthStateManager` | port in `outboundservices/oauth`; controller depends on the port |
| `domain/services/GoogleTokenVerifier` | outbound port → `application/internal/outboundservices/oauth/` |

---

## Phase 7 — device

Largest; last, when the pattern is proven six times.

| Today | Target |
|---|---|
| `Device`, `Organization`, `Space` (`@Entity`) | `domain/model/aggregates/`; three `*PersistenceEntity` |
| `DeviceAssignment`, `DeviceCommand` with `@ManyToOne(LAZY) Device device` and own repositories | they are de facto aggregates: `domain/model/aggregates/`, referencing the device by `DeviceId` value, not object. Persistence entity keeps the `device_id` FK column so DDL is unchanged. This removes the open-in-view dependency in `EdgeCommandController.toResource` and `DeviceContextFacadeImpl.findDeviceIdsBySpaceId` |
| `DeviceAssignment.configuration: Map<String,String>` (`@ElementCollection`, holds threshold JSON) | the assembler round-trip test **must** assert a map with ≥2 entries; this is the field most likely to vanish silently |
| VOs `HardwareId`, `ApiKey`, `ClaimToken`, `DeviceType`, `UserId` (single), `DeviceMetricThresholdConfiguration` (multi) | five converters, one embeddable |
| ten `domain/services/*` | `application/*services/` |
| `DeviceRosterController` injecting `DeviceRepository` and re-implementing token auth inline | `GetProvisionedDevicesQuery` (exists); remove inline check, `ServiceTokenAuthenticationFilter` already covers `/api/v1/edge/**` |
| `DeviceEdgeController` + `DevicePresenceSecurityConfiguration` (second, unauthenticated filter chain) | delete both; `EdgePresenceController` under `/api/v1/edge/presence` already exists and is token-protected. **This is also the security fix; if it cannot wait for Phase 7, do it alone before Phase 0** |
| `EdgeCommandAcknowledgementService` taking `EdgeCommandAckRequest` | `AcknowledgeEdgeCommandCommand` |
| `EdgeCommandController` hand-building `Map<String,Object>` | `EdgeCommandResource` + assembler |
| `ThresholdContextFacade` exposing domain VOs | already switched to `ThresholdSummary` in Phase 4 |
| `Device.DeviceAudit` (`@Embeddable`) with hand-rolled `touchUpdatedAt()` in four places; `DeviceRepository.findProvisionedDevices` pages on `updated_at` as a cursor watermark | `DevicePersistenceEntity extends AuditableAbstractPersistenceEntity`. Auditing already fills both timestamps through the embeddable (see the corrected note in Phase 1), so the move itself changes nothing. What still needs deciding is `touchUpdatedAt`: it forces `updated_at` forward on changes Hibernate does not see as dirty, and dropping it would change what the edge roster cursor returns. Assert the pagination behaviour explicitly either way, the gate cannot |
| `DeviceSecretColumnDropMigration` (`ALTER TABLE` in `@PostConstruct`) | delete once run everywhere; Phase 8 |
| `*Request`/`*Response`, singular controller names | `*Resource`, plural names |

---

## Phase 8 — shared cleanup, after every context is split

- Delete `shared/domain/model/entities/AuditableModel` (no subclasses remain).
- `AuditableAbstractPersistenceEntity`: delete the two `@JdbcTypeCode(SqlTypes.TIMESTAMP)` annotations so the `Instant` fields map to `timestamp with time zone`. Regenerate DDL, accept the column-type diff explicitly, once, and write the `ALTER TABLE` migration for every table — `ddl-auto: update` does not change an existing column's type.
- `spring.jpa.open-in-view: false`. Nothing should surface; if something does, it is a missed lazy navigation and is fixed here.
- Replace `ddl-auto: update` with Flyway `V1__baseline.sql` generated from the final DDL and `ddl-auto: validate`. Delete `DeviceSecretColumnDropMigration`.
- Listeners: `@EventListener` → `@TransactionalEventListener(phase = AFTER_COMMIT)` for the four cross-context consumers. This changes event timing; it is the intended behaviour and is isolated here so it can be reverted alone.
- `EdgeEventPublisher`: extract port `shared/application/outboundservices/EdgeNotifier`; add HTTP timeouts.

## Phase 9 — ArchUnit

Written last so it locks a state that already holds. Rules, in prose:
1. No class under `..domain..` depends on `jakarta.persistence..`, `org.springframework.web..`, `org.springframework.data.jpa..`, `org.springframework.stereotype..`, `com.fasterxml..`.
2. No class under `..domain..` depends on `..application..`, `..infrastructure..`, `..interfaces..`.
3. No class under `..application..` depends on `..infrastructure..` or `..interfaces.rest..`.
4. No class under `..interfaces..` depends on `..infrastructure..`.
5. No class under `com.claircore.shared..` depends on any named context package.
6. Slices `com.claircore.(*)..` do not depend on each other except through `..interfaces.acl..` and `..interfaces.events..`.
7. Classes named `*PersistenceAssembler` have only private constructors and only static methods.

Version of `archunit-junit5`: look up on Maven Central when writing the test; do not copy from memory.

## Order and dependencies

0 → 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9, strictly. Phase 4 touches device only to add one facade method and change one facade return type; those two edits are the sole exception to "one context per phase". The security fix in Phase 7 may be pulled forward to before Phase 0 as its own phase.

If a phase cannot be reverted cleanly, it did more than one thing.