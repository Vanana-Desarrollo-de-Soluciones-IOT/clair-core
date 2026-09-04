# Refactor Branch Plan — clair-core

Derived from `01-audit-report.md` (baseline `204b7cc`). Follows §9 of the audit guide: one concern per branch, each branch compiles and boots at its tip, Conventional Commits, no branch mixes a move with a behaviour change. Branch numbering matches §9 so the two documents can be read side by side; branches marked **(adapted)** differ from the template because of what the audit actually found.

Current repo state: branch `refactor/domain-purity` exists locally and on origin but has **zero commits ahead of `main`**. `refactor/domain-dependency` also exists (not inspected). Neither should be reused as-is; start from the plan below and delete or rename the empty branches.

## Sizing

| Metric | Value | Source |
|---|---|---|
| Files with §4.1 violations | 48 | report §A |
| Aggregates to split | 17 (15 JPA, 2 Redis) | report §D |
| Domain ports to create | 15 | report §D |
| Assemblers to create | 17 | report §E |
| Cross-context edges to cut | 12 | report §C |
| Existing tests | 152 test classes | `src/test` |

The 152 existing tests are the safety net. Most are unit tests on domain records and application services with mocked repositories. **Branches 3 and 4 will break every service test that mocks a Spring Data repository**; budget for a mechanical `Mockito.mock(XRepository.class)` → `mock(XRepository.class)` (now the port) rename per test, nothing more.

---

## Branch sequence

### 0. `chore/audit-baseline`
**Content.** Commit `docs/audit/01-audit-report.md`, this file, and the two domain-improvement documents. No source changes.
**Done when.** Merged to `main`. Every later branch's PR links to the report section it closes.

---

### 0b. `fix/ingestion-auth` **(new, security, runs first)**
Not in §9 but the audit found two BLOCKER security defects (report J3, J4) that do not depend on any refactor and must not wait 13 branches.

**Content.**
1. Add `/api/v1/evaluations/telemetry` to `ServiceTokenAuthenticationFilter.PATHS` **or** (preferred, see `04-domain-improvements-iot-ingestion.md` §2) add a `DeviceApiKeyAuthenticationFilter` that validates `X-Device-Key` against `Device.apiKey` for the hardware id in the body.
2. Delete `device/infrastructure/config/DevicePresenceSecurityConfiguration` and `DeviceEdgeController`; `EdgePresenceController` under `/api/v1/edge/presence` already exists and is token-protected.
3. Remove dead `permitAll` rules for `/api/v1/devices/provisioning`, `/api/v1/devices/commands/pending` (report J6).
4. Remove the inline token check from `DeviceRosterController` (report J5); the filter already covers `/api/v1/edge/**`. Keep the `X-Edge-Token` fallback only in the filter, with a removal date.

**Done when.** `curl -X POST /api/v1/evaluations/telemetry` without a token returns 401; the edge repo's telemetry publisher is updated to send the header; `EdgePresenceController` test covers 401.

---

### 1. `refactor/domain-purity` **(adapted)**
§9 says "remove every banned import from `domain/`; no behaviour change". In this codebase that is impossible in one step because **removing `@Entity` from `Device` breaks persistence** — there is no separate entity to move the annotations to. So branch 1 is narrowed to what *can* be done without a persistence entity, and the annotation removal moves to branch 2.

**Content.**
1. Create `shared/domain/model/aggregates/AbstractDomainAggregateRoot<T>` (wraps `o.s.data.domain.AbstractAggregateRoot`, the documented exception). Make `UserPlan`, `PaymentRecord` extend it instead of Spring's class directly.
2. Move `shared/domain/model/entities/AuditableModel` → `shared/infrastructure/persistence/jpa/entities/AuditableAbstractPersistenceEntity`. Every nested `XAudit extends AuditableModel` class updates its import. (Still a domain→infrastructure import at this point; it disappears in branch 2. Record this as a known intermediate state in the PR.)
3. Remove `@Service` from the three `analytics/domain/services/*DomainServiceImpl` (report A #11-13). Register them as beans from a `analytics/infrastructure/config/AnalyticsDomainServicesConfiguration` `@Configuration` with `@Bean` methods. This is the §1-conformant way to have a framework-free domain service.
4. Replace `o.s.data.domain.Page`/`Pageable` in domain ports and query records (report A #49, #50) with a `shared/domain/model/PageResult<T>` record and `page`/`size` ints. Application impls convert at the boundary.
5. `SubscriptionPaidEvent`, `UserRegisteredEvent`: convert from `extends ApplicationEvent` to plain records (report A #51). Publishers pass the record to `ApplicationEventPublisher.publishEvent(Object)`; listeners change parameter type. Behaviour identical.
6. Remove `@Email` from `EmailAddress` (report A #55).
7. Delete dead files listed in report §K "dead-code inventory" **except** the outbox (decided in branch 9).

**Done when.** `grep -rE 'org.springframework.(stereotype|web|context.ApplicationEvent|data.domain.Page)' src/main/java/**/domain` returns nothing; only `jakarta.persistence` and `AuditingEntityListener` remain (tracked to branch 2). App boots; 152 tests pass.

---

### 2. `refactor/persistence-entities`
**Content.** For each of the 15 JPA aggregates in report §D, create `infrastructure/persistence/jpa/entities/<X>PersistenceEntity extends AuditableAbstractPersistenceEntity` that carries **every** JPA annotation currently on the aggregate, with identical `@Table`, `@Column`, `@Index`, `@UniqueConstraint`, `@AttributeOverride` values. The aggregate loses all annotations and becomes a plain class extending `AbstractDomainAggregateRoot`. Value objects lose `@Embeddable`; for each VO used in a column, either a `<VO>PersistenceEmbeddable` (multi-field: `AirQuality`, `ParticulateMatter`, `Connectivity`, `MetricStats`, `AqiCategoryBreakdown`, `AirQualityIndex`, `MetricTrend`, `Money`, `DeviceMetricThresholdConfiguration`) or a `<VO>PersistenceConverter` (single-field: `DeviceId`, `UserId`, `HardwareId`, `ApiKey`, `ClaimToken`, `DeviceType`, `EmailAddress`, `Password`, …) is created.

The two Redis aggregates (`RegistrationSession`, `TokenSession`) get `infrastructure/persistence/redis/entities/<X>RedisDocument` records carrying the Jackson annotations; the domain records lose them (report A #54).

Nested-entity decision, recorded here: `DeviceAssignment` and `DeviceCommand` keep their own repositories and become **aggregates** (`domain/model/aggregates`), referencing `Device` by `DeviceId` value, not by object. `DeviceAssignment.device: Device` → `deviceId: DeviceId`. This removes both `@ManyToOne` and the open-in-view dependency (report H6). The persistence entity keeps the FK column `device_id`.

**Constraint.** In this branch the persistence entity is created but **nothing uses it yet**; Spring Data repositories still point at the aggregate. That does not compile once the aggregate loses `@Entity`. So branch 2 must land **together with a minimal version of branches 3–5 for one context at a time**, or the aggregate keeps `@Entity` until branch 4. Recommendation: **execute branches 2–5 per bounded context**, smallest first, each as its own PR series:

| Order | Context | Aggregates | Why this order |
|---|---|---|---|
| 2a | notifications | `EmailLog`, `PushNotificationLog` | Already has a (partial) domain port; 2 aggregates; no cross-context readers of its tables. |
| 2b | evaluation | `TelemetryEvaluation` | 1 aggregate, but its **column names are read by raw SQL in analytics** — the DDL diff gate matters most here. |
| 2c | billing | `UserPlan`, `PaymentRecord` | Already uses `AbstractAggregateRoot`; Stripe webhook test exists. |
| 2d | alerting | `Alert` | JPQL joins `Device` (report C11) — fix in branch 10, but the JPQL must move to a native query on `devices.hardware_id` here to keep compiling. |
| 2e | analytics | 3 summaries/snapshots | Writers are schedulers, not command services (see branch 6). |
| 2f | iam | `User` + 2 Redis | Jackson removal; most tests (40+). |
| 2g | device | 5 aggregates | Largest; `configuration` map collection (report E). Last, when the pattern is proven. |

**Done when (per context).** Generated DDL for that context's tables is byte-identical before/after. Capture with `spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create` to a file on `main` and on the branch tip; `diff` must be empty except for the removed `outbox_message` if branch 9 lands first. This is the control §0.7 demands.

---

### 3. `refactor/repository-ports`
**Content (per context, paired with 2x).** Create `domain/repositories/<X>Repository` interfaces expressed in domain types (`Optional<Device> findByHardwareId(HardwareId)`, `PageResult<Alert> findByDeviceId(DeviceId, int page, int size)`). Rename Spring Data interfaces to `<X>PersistenceRepository` (report I7) and retarget them at `<X>PersistenceEntity`. Method bodies of `@Query` annotations are not rewritten; only entity/field names in JPQL change where the entity class was renamed.

The notifications context already has `EmailLogPersistence` and `PushNotificationHistoryRepository` under `domain/repositories`, with the Spring Data interface *extending* the port. Undo that: the port is implemented by an adapter (branch 4), never extended by Spring Data.

**Done when.** No class outside `infrastructure/` imports `JpaRepository` or `*PersistenceRepository`. `grep -rl PersistenceRepository src/main/java | grep -v infrastructure` is empty.

---

### 4. `refactor/persistence-adapters`
**Content.** `infrastructure/persistence/jpa/adapters/<X>RepositoryImpl implements <X>Repository`, `@Repository`, delegating to `<X>PersistenceRepository` and calling the assembler from branch 5. For Redis: `iam/infrastructure/persistence/redis/adapters/TokenSessionRepositoryImpl` wrapping the current concrete class (which becomes package-private). Application services switch their constructor parameter type from the Spring Data interface to the port. **Every ACL facade impl** (`*ContextFacadeImpl`) switches from repository to its context's query service (report F6); `EvaluationContextFacadeImpl` and `DailyReportAggregationService` `JdbcTemplate` usage moves into an adapter method on `TelemetryEvaluationRepository` (`List<HourlyDeviceAverage> aggregateHourly(Instant, Instant)`) so the raw SQL lives in infrastructure (report C12, F7).

**Done when.** No class outside `adapters/` names both a domain aggregate and a `*PersistenceEntity`. All 152 tests green after the mock-type rename.

---

### 5. `refactor/assemblers`
**Highest defect risk. This branch does nothing else.**
**Content.** `infrastructure/persistence/jpa/assemblers/<X>PersistenceAssembler`: `final`, private constructor, exactly `toDomainFromPersistence` / `toPersistenceFromDomain`, null-safe. Aggregates gain a `static <X> reconstitute(...)` factory (the `User.rehydrate` pattern, report L1) taking every field including id and timestamps; **no setters**. Assemblers delegate for nested types and use the converters/embeddables from branch 2 instead of unwrapping VOs (§5 Step 6.7).

For every aggregate, a `<X>PersistenceAssemblerTest` round-trips a fully populated aggregate `domain → persistence → domain` and asserts field-by-field equality, **including `DeviceAssignment.configuration` with ≥2 entries** (report E row 1) and every nullable column set both to a value and to null.

**Done when.** Per-assembler round-trip test exists and passes for all 17; report §E table is filled in with `assigned? = yes` for every row.

---

### 6. `refactor/split-command-query` **(adapted: it's a move, not a split)**
This codebase already separates command and query services. The deviation is placement (report I1): ports live under `domain/services`.
**Content.** Move `<ctx>/domain/services/*CommandService` → `<ctx>/application/commandservices/`, `*QueryService` → `<ctx>/application/queryservices/`. Analytics domain services (`AqiCalculationDomainService` etc.) **stay** in `domain/services` — they are real domain services. Pure `git mv` + import fixes.

Also: analytics' three scheduler classes (`DailyReportAggregationService`, `MonthlyReportAggregationService`, `SnapshotAggregationScheduler`) become `application/internal/commandservices/*CommandServiceImpl` behind commands `GenerateDailySummaryCommand(LocalDate)`, `GenerateMonthlySummaryCommand(YearMonth)`, `AggregateHourlySnapshotCommand(Instant windowStart)`; a thin `application/internal/schedulers/AnalyticsScheduler` holds the `@Scheduled` methods and calls the ports. **This fixes the self-invocation bug (report H3) as a side effect of the move** — the `@Transactional` is now on a different bean. Document that in the PR since §9 says no behaviour change; here the behaviour *becomes correct* without a logic edit.

**Done when.** `domain/services` contains only `*DomainService` interfaces and impls in analytics, nothing elsewhere. `internal/services` is empty and deleted.

---

### 7. `refactor/commands-and-queries`
Already conformant: commands and queries live under `domain/model/{commands,queries}` in every context. **Skip**, or fold the tiny remainder into branch 12: `GetDashboardMetricsQuery.period: String` → `TrendPeriod` (report I18).

---

### 8. `refactor/rest-transform`
**Content.** Add `interfaces/rest/transform/<X>ResourceFromEntityAssembler` and `<Cmd>CommandFromResourceAssembler` for device, alerting (replacing static `from()` on resources), and for the two edge controllers that hand-build `Map<String,Object>` (`EdgeAlertController.resource`, `EdgeCommandController.toResource`) — give them `EdgeAlertResource` / `EdgeCommandResource` records. `EdgeAlertController` and `DeviceRosterController` stop injecting repositories (report B2-1, B2-2): add `GetPendingEdgeAlertsQuery` / `GetProvisionedDevicesQuery` (the latter exists) to the query services. Edge ack services stop taking REST DTOs (report B1-a, B1-b): introduce `AcknowledgeEdgeAlertCommand`, `AcknowledgeEdgeCommandCommand`. Controllers stop importing `JwtAuthenticationFilter` (report B2-4..8): introduce `shared/interfaces/rest/security/CurrentUserId` argument resolver.

**Done when.** No controller imports `infrastructure.*` or names a `*PersistenceEntity`/`*Repository`.

---

### 9. `refactor/shared-extraction`
**Content.**
1. `GlobalExceptionHandler` → `shared/interfaces/rest/GlobalExceptionHandler`, returning `ErrorResource` record; stop importing `analytics.domain.exceptions` (report C1) by having `DeviceTelemetryUnavailableException extends shared.domain.exceptions.ResourceNotFoundException`.
2. `EdgeEventPublisher`: extract port `shared/application/outboundservices/EdgeNotifier { void notifyChange(String resource, String hint); }`; impl stays in `shared/infrastructure/edge`, gains `RestTemplateBuilder` timeouts (report J12). Publishers in device/alerting inject the port.
3. Decide the outbox (report G): **delete** `OutboxMessage*` in this branch. If reliable edge notification is wanted later, `04-domain-improvements-iot-ingestion.md` §5 describes reintroducing it deliberately.
4. `OpenApiConfiguration` → `shared/infrastructure/documentation/openapi/configuration/`.

**Done when.** `grep -rE 'import com.claircore.(alerting|analytics|billing|device|evaluation|iam|notifications)' src/main/java/com/claircore/shared` is empty.

---

### 10. `refactor/context-acl`
**Content.** Cut every edge in report §C.
1. `ThresholdContextFacade` returns a DTO `ThresholdSummary(String metric, BigDecimal value, boolean enabled)` instead of `DeviceMetricThresholdConfiguration`; alerting maps to its own `MetricType` (C2–C4).
2. `AlertingContextFacade.getRecentAlertsByOwnerId` takes `List<String> statuses` (C7, C8).
3. Integration events: evaluation publishes `evaluation/interfaces/events/TelemetryRecordedIntegrationEvent`; alerting & analytics listeners consume **that** and delete their local unused copies (C5, C6). Alerting publishes `alerting/interfaces/events/AlertIncidentChangedIntegrationEvent` (move from `outboundservices/acl`); notifications consumes it (C9). iam publishes `iam/interfaces/events/UserRegisteredIntegrationEvent`; billing consumes it (C10).
4. `AlertRepository.findPendingForEdge` JPQL join on `Device` (C11): the alerting adapter calls `DeviceContextFacade.findHardwareIdsByDeviceIds(List<UUID>)` (new batch method) instead of joining. Add the batch method to the device facade first.
5. Listeners switch to `@TransactionalEventListener(phase = AFTER_COMMIT)` (report H7, H8). This *is* a behaviour change (events fire after commit instead of inside the transaction) — it is the intended behaviour and is documented as such in the PR; it is isolated in this branch so it can be reverted alone.

**Done when.** For every file, `import com.claircore.<other-ctx>.` matches only `.interfaces.acl.` or `.interfaces.events.`. Report §C is empty.

---

### 11. `fix/transactions`
**Content.** What remains after branch 6 fixed H3 and branch 10 fixed H7/H8:
1. `OverviewDashboardQueryServiceImpl`: remove `@Transactional(readOnly=true)` from a method that fans out to other threads; pass the `taskExecutor` bean to `supplyAsync` (report F3).
2. Remove `@Transactional` from ACL facades (report H2).
3. `spring.jpa.open-in-view: false` in `application.yml`; fix the two lazy navigations that surface (report H6). After branch 2g they no longer exist, so this should be a no-op verification.
4. Delete `DeviceSecretColumnDropMigration`; adopt Flyway with a `V1__baseline.sql` generated from the current DDL and `ddl-auto: validate` (report H10, J7). This is the one infrastructure addition in the plan; it is what makes the "DDL unchanged" gate of branch 2 permanent.

**Done when.** Each write path's rollback behaviour is written down in the PR (table: endpoint → transaction boundary → what rolls back on failure) and manually exercised for telemetry ingestion and device claim.

---

### 12. `chore/naming-conformance`
**Content.** Everything in report §I not already done: plural controller names, `*Request/*Response` → `*Resource`, `*Transform` → `*Assembler`, `inboundservices/acl` → `eventhandlers`, `aggregates/` vs `entities/` placement, `DailyAlertCount` relocation, `StaticWebController` behind a `demo` profile, remove duplicate `DeviceEdgeController` if 0b did not, `AuditableModel` `Date` → `Instant`. Zero logic change; IDE refactor-rename only.

**Done when.** Layout matches §2 and §3; `git diff --stat` shows only renames (`-M` similarity ≥ 90 %).

---

### 13. `build/annotation-processing`
Report J1/J2: nothing to fix. **Skip.** Use the slot for `build/test-profile` instead: add `src/test/resources/application-test.yml` pointing at H2 (already on the classpath) and Redis-less caching (`spring.cache.type=simple`) so `@SpringBootTest` classes can run in CI (report J10); mark `spring-boot-devtools` `<optional>true</optional>` (report J11).

---

### 14. `test/architecture-rules`
**Content.** Add `com.tngtech.archunit:archunit-junit5` (look up the current version on Maven Central at branch time; do not copy from memory). Rules:
- `noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.springframework.web..", "org.springframework.data.jpa..", "org.springframework.stereotype..")`
- `noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..", "..interfaces..")`
- `noClasses().that().resideInAPackage("..application..").should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "..interfaces.rest..")`
- `noClasses().that().resideInAPackage("..interfaces..").should().dependOnClassesThat().resideInAPackage("..infrastructure..")`
- `noClasses().that().resideInAPackage("com.claircore.shared..").should().dependOnClassesThat().resideInAnyPackage("com.claircore.alerting..", …)`
- A slice rule: `slices().matching("com.claircore.(*)..").should().notDependOnEachOther().ignoreDependency(…interfaces.acl…, …interfaces.events…)`.
- `classes().that().haveSimpleNameEndingWith("PersistenceAssembler").should().haveOnlyFinalFields().andShould().haveOnlyPrivateConstructors()`.

**Done when.** All rules pass on `main`. Written last so it locks a state that already holds.

---

## Merge discipline

- **0 → 0b → 1** strictly sequential.
- **2a–2g** each carry their own 3/4/5 work for that context; contexts are independent and may be parallelised across people once 2a has established the pattern. Order within a context: entity → port → adapter → assembler, each a separate commit, one PR per context.
- **6, 8, 9, 10** may run in parallel after all of 2 lands. 10 depends on 9 (needs `interfaces/events` convention and `EdgeNotifier` port).
- **11** after 10. **12** after 11. **13** anytime. **14** last.
- Rebase within a branch; squash-merge is acceptable for 12 only.

## Rollback rule

If a branch cannot be reverted cleanly, it did more than one thing. Split it. The two places this plan knowingly bends the rule are called out inline (branch 6 fixes H3 by moving; branch 10 changes event timing) and are each isolated so that a revert of that one branch restores the previous behaviour.
