# clair-core — Behaviour Backlog

Everything here changes what the system does. None of it was part of the structural refactor, and none of it may share a branch with a structural phase, because a structural phase is verified by "DDL unchanged, tests unchanged" and behaviour work breaks both gates by design.

The structural refactor is finished: phases 0–9 are merged on `refactor/domain-purity`. Its planning
and completion records (`refactor.md`, `current-state.md`, the per-phase DDL snapshots) were removed
from the working tree once the work landed; recover any of them with
`git show 5da6b2e:docs/audit/refactor.md`. This backlog is the remaining domain-behaviour work — it
is not a list of unresolved repairs from that refactor.

## Rule

An item may start only when the structural phase it names is merged. Each item is one branch, one `fix:` or `feat:` commit series, with the test named in its row written **first** as the control.

## Before Phase 0 (independent of structure)

| # | Item | Source | Control |
|---|---|---|---|
| ~~B0.1~~ | **Done in Phase 7.** Both classes are deleted and `ServiceTokenAuthenticationFilter.PATHS` is `{"/api/v1/edge/**", "/api/v1/evaluations/telemetry/batch"}` — the real route carries the `/batch` suffix the row omitted. | I§1 | request without token → 401 |
| B0.2 | AQI breakpoint gaps: make ranges contiguous, clamp above the last bound. Still open. | A§1 | `AqiCalculatorTest` sweeps 0..1000 step 0.01, asserts monotonic |
| ~~B0.3~~ | **Obsolete.** The internal `TelemetryRecordedEvent` was deleted in Phase 5; consumers read evaluation's `TelemetryRecordedIntegrationEvent`, whose `from(...)` maps `pm10` from `pm10()`, carries no `hardwareId` field, and keeps `healthStatus` as an `int`. All three defects are gone with the class. | I§13 rows 1–3 | event field test |

## After Phase 2 (evaluation split)

| # | Item | Source | Control |
|---|---|---|---|
| B2.1 | Idempotent ingestion: edge-supplied `ReadingId` with unique constraint; fallback unique `(device_id, measured_at)`; duplicate → `DUPLICATE`, no event. | I§3 | replay a batch twice, row count unchanged |
| B2.2 | Single ingestion contract `POST /api/v1/edge/telemetry` with typed batch request, 207 response; deprecate the two old routes. Coordinate with the edge repo. | I§2 | contract test on the new route |
| B2.3 | Three explicit instants `measuredAt`, `receivedByEdgeAt`, `receivedByCoreAt`; drop `deviceTime: LocalTime`. **Schema change**, needs Flyway from Phase 8 or an explicit accepted DDL diff. | I§6 | analytics buckets by `measuredAt` |
| B2.4 | `Measurement` VOs with physical ranges; `ReadingQuality { VALID, SUSPECT, INVALID }`; PM as `Double` end to end. | I§5 | out-of-range reading stored as INVALID, not published |
| B2.5 | Rename context `evaluation` → `telemetry`, aggregate `TelemetryReading`. Pure rename, do it last in this group so the branches above are reviewable. | I§4 | compile |

## After Phase 5 (analytics split)

| # | Item | Source | Control |
|---|---|---|---|
| B5.1 | One AQI aggregation rule (`AqiAggregationPolicy`, exposure-weighted); delete the three accumulators' divergent maths. | A§2 | dashboard and daily report agree on a fixture day |
| B5.2 | One `MetricTrend` with `comparisonBasis`; one rounding rule in transforms; `null` never fake `0`. | A§7 | |
| B5.3 | Live window to Redis (`LiveTelemetryWindow` port, sorted-set adapter); SSE fan-out via Redis pub/sub. | A§4 | two instances report the same `Freshness` |
| B5.4 | Analytics owns `DeviceHourlyBucket`, fed incrementally from `TelemetryRecordedIntegrationEvent`; daily/monthly roll up from buckets; analytics never reads `telemetry_evaluations`. `ReportingCalendar` (zone) comes from the organization, not `@Value`. | A§3 | daily total equals sum of 24 buckets |
| B5.5 | `SpaceAnalyticsProjection` maintained from device integration events; overview is one query; remove `CompletableFuture` from the read-only transaction. | A§6 | facade method count drops; overview latency independent of space count |
| B5.6 | Read models to `domain/model/readmodels`; premium check moves from `ReportController` to `MonthlyReportQueryService` via a `ReportAccessPolicy` port; empty result → `Optional`, not exception. | A§9 | |

## After Phase 7 (device split) and Phase 8 (events after commit)

| # | Item | Source | Control |
|---|---|---|---|
| B7.1 | `DeviceModel` (capability catalogue): expected measurements, sampling interval, heartbeat timeout, sensor ranges. `Device.firmwareVersion`. | I§7 | |
| B7.2 | Presence split into `connectivity`, `powerMode`, `lifecycle`; core-side timeout `DetectSilentDevicesCommand`; telemetry arrival advances `lastSeenAt`; emit `DeviceWentSilent` / `DeviceCameBack`. Analytics deletes `Freshness` and asks the device facade. | I§8, A§5 | device streaming readings is never OFFLINE |
| B7.3 | `AlertRule` aggregate in alerting (comparator, threshold, hysteresis, cooldown); delete `DeviceAssignment.configuration` threshold JSON and `ThresholdContextFacade`; one `MeasurementKind` enum in `shared/domain`. | I§9, A§8 | malformed rule cannot silently disable alerting |
| B7.4 | Command lifecycle: `expiresAt`, `EXPIRED`, `CANCELLED`, one `acknowledge(...)` behaviour, delete `DispatchPendingDeviceCommandsCommand`. | I§10 | stale command stops being re-delivered |
| B7.5 | Provisioning: seeder behind `demo` profile or CSV import; quotas from billing `PlanQuota` only; `apiKey` stored hashed. | I§11 | |
| B7.6 | Device-plane authentication: `DeviceCredentials`, `DeviceContextFacade.authenticate(hardwareId, apiKey)`, `DeviceAuthenticationFilter`. Completes B0.1. | I§1 | forged hardware id without key → 401 |

## After Phase 8 (Flyway in place)

| # | Item | Source | Control |
|---|---|---|---|
| B8.1 | Partition `telemetry_*` by month; `RetentionPolicy` per plan; `PurgeExpiredReadingsCommand`. | I§12 | migration applies on a copy of production data |

## Features (A§10)

Time-in-category, data completeness, ventilation insight, space/organization reports, peak-hour profile, alert-aware reports, comparative baseline, standard-labelled AQI. Each depends on B5.4 and B7.1; none is scheduled until those two exist.

## Dropped from the original documents

Nothing dropped. Two items moved: I§1 telemetry token (now B0.1) and A§1 (now B0.2) run before Phase 0 because they touch one file each and do not depend on structure.

## Device review — 2026-09-07

Scope: device aggregates, application services, REST/security boundaries, persistence, and representative tests. Findings below come from source inspection; concurrency outcomes need the database controls specified below. P1 means ownership/integrity or core business-rule failure; P2 means lifecycle correctness or maintainability. Existing B7 items remain open; references below refine them rather than replace them.

### Architecture assessment

The structure supports DDD: framework-free domain objects, repository ports, separate JPA entities/assemblers, ID references between aggregates, and published ACL/event contracts between contexts. `ArchitectureTest` enforces these dependency boundaries. Passing those rules does not establish correct aggregate invariants or tenant isolation.

The principal gaps are behavioral: `DeviceAssignment` owns claim state but concurrent claims are not serialized; `DeviceCommand` exposes unrestricted terminal mutations while application services enforce different transition rules; `Space.ownerUserId` duplicates organization ownership without enforcing agreement. Device lifecycle, assignment, and outstanding commands need an explicit consistency policy. Keeping separate aggregates is reasonable, provided application transactions enforce that policy. Threshold JSON and mixed presence/power state remain the modeling debt already tracked in B7.2/B7.3.

Paths in the following table are relative to `src/main/java/com/claircore/`.

| # | Priority / evidence | Finding and intended change | Control to write first |
|---|---|---|---|
| B7.7 | **P1 — confirmed missing authorization** | `device/interfaces/rest/controllers/SpaceController` passes no actor to delete/rename; `SpaceCommandServiceImpl` performs no owner check. Space creation loads the organization but never checks its owner. `DeviceController.getDevices` and space reads use unscoped queries in `DeviceQueryServiceImpl`. `iam/infrastructure/config/SecurityConfiguration` requires authentication, not resource ownership. Enforce actor-scoped authorization in application entry points and require organization/space owner consistency; preserve explicitly trusted internal reads. | With users A/B, B cannot read A's space/device list, rename/delete A's space, or create a space under A's organization. Exercise real security/controller/service wiring; assert no mutation. |
| B7.8 | **P1 — concurrency risk** | `DeviceCommandServiceImpl.handle(ClaimDeviceCommand)` uses unlocked `findByClaimToken`; `DeviceAssignmentPersistenceEntity` and its base have no version. Two transactions can read the same unclaimed assignment and overwrite ownership, each receiving success. Lock the claim row before checking/consuming its token, or use an atomic conditional update/version with a defined conflict result. Coordinate with other assignment writers to prevent stale state overwrites. | PostgreSQL, two transactions synchronized after token lookup: exactly one claim succeeds, the loser receives a domain conflict, and the winner's ownership remains stored. |
| B7.9 | **P1 — confirmed missing quota enforcement; refines B7.5** | `DeviceCommandServiceImpl.handle(ClaimDeviceCommand)` never calls `ExternalBillingService.getMaxDevices` or counts owned devices. The injected billing service is unused there. Enforce the billing device quota at claim time, with serialization at the quota owner boundary so simultaneous claims cannot exceed it. | At quota, another claim fails without consuming its token; with one slot left, two concurrent claims produce exactly one success. |
| B7.10 | **P1 — confirmed lifecycle gap; refines B7.4** | Reset deletes only the assignment in `DeviceCommandServiceImpl`; commands retain only `deviceId`. Pending/SENT commands remain deliverable through `EdgeCommandServiceImpl`, and a late ACK applies to whichever assignment currently has that device ID. An old owner's STANDBY can therefore affect a new owner. Bind commands to an assignment generation, cancel outstanding commands on unlink, serialize creation with reset, and reject stale ACK side effects. | A queues a command, unlinks, B pairs/claims: A's command is not returned for delivery and its delayed ACK cannot change B's assignment. Include create-versus-reset race. |
| B7.11 | **P2 — confirmed divergent contracts; refines B7.4** | `DeviceCommand.markExecuted/markFailed` allow transitions from any state. `DeviceControlCommandServiceImpl` ACK uses an unlocked lookup and bypasses the SENT/terminal guards present in `EdgeCommandServiceImpl`. No REST caller of the legacy ACK was found in this review, so this is a latent application API defect, not a demonstrated public endpoint bypass. Consolidate transitions in one aggregate behavior and one locking policy; remove unused legacy dispatch/ACK entry points after checking callers. | Reject PENDING→EXECUTED and terminal-state rewrites; duplicate/conflicting ACK policy is explicit and consistent across every remaining entry point, including concurrent ACKs. |
| B7.12 | **P2 — confirmed ordering gap; refines B7.2** | `DeviceAssignment.updatePresence` always overwrites status and accepts older/future timestamps. An older ONLINE can move `lastSeenAt` backward; an older OFFLINE can override a newer ONLINE. OFFLINE has no ordering watermark at all. Track last applied presence event time separately from last seen, define duplicate/out-of-order handling and allowable clock skew, and pass time explicitly for deterministic behavior. | Deliver ONLINE(t2), OFFLINE(t1), ONLINE(t0), where t0<t1<t2: status and freshness do not regress. Also cover a newer OFFLINE, duplicate events, and excessive future timestamps. |
| B7.13 | **P2 — concurrency risk** | Pairing locks an existing assignment via `findByDeviceIdForUpdate`, but there is no assignment row to lock on first pairing. Two first-pair requests can both insert; the unique `device_id` constraint prevents duplicates but one request can fail instead of returning the existing claim token. Serialize on the inventory device row or handle insert conflict with a fresh transaction/read and a defined result. | Concurrent first-pair requests create one assignment and return the agreed idempotent result, with no unhandled persistence exception. |

Suggested order: B7.7/B7.8, then B7.9/B7.10, then B7.11–B7.13. Implement behavior fixes separately from this documentation review.

### Test value and redundancy

Local validation: `mvn -q test` completed successfully on 2026-09-07. Surefire reports **602 tests: 598 passed, 0 failures/errors, 4 skipped**. The skipped tests are `PostgresRosterIntegrationTest` (1) and `MigrationIntegrationTest` (3); this run does not establish PostgreSQL migration/locking correctness. Source inventory has 203 device test methods: 95 domain, 52 application, 30 REST, and 26 persistence. These are counts, not coverage percentages.

**All 602 are not automatically necessary.** Review each test by the distinct plausible defect it detects, the boundary it exercises, and whether a retained test would detect that same defect reliably. Count and line coverage alone are insufficient. Parameterizing cases reduces duplicated code, not necessarily executed test count.

| Category / example | Decision criterion |
|---|---|
| `GetDeviceByIdQueryTest.shouldCreateQueryWhenDeviceIdIsValid`, `UpdateDeviceNameCommandTest.shouldCreateCommandWhenValuesAreValid` | Pruning candidates: only verify generated record accessors preserve constructor inputs. Retain custom validation/transformation cases; remove happy-path accessor-only tests after confirming another meaningful use exercises construction. |
| Constructor rejection tests and exact exception-message assertions | Keep distinct null/blank/range rules. Parameterize equivalent invalid inputs where it improves readability. Avoid pinning message wording unless it is an intentional public contract; exception type and rejected behavior usually matter more. |
| `DeviceAssignmentTest` claim/presence tests versus `DeviceAssignmentRepositoryImplTest` round trips | Not automatically redundant: aggregate tests protect state transitions; database tests protect converters, mapping and storage. Keep both purposes. Strengthen persistence tests with flush/clear before reload where needed, so first-level cache cannot masquerade as a stored round trip. |
| `DeviceAssignmentRepositoryImplTest.updatingPresenceMovesTheTimestampButOfflineDoesNot` | Mixed responsibilities: move the OFFLINE state-rule assertion into aggregate tests; keep a precise timestamp persistence assertion if other database tests do not already cover it. |
| Architecture, security, migrations, edge lease/ACK tests | Keep tests with distinct boundary risks. Mock-only service tests cannot prove authorization wiring, SQL constraints, transaction isolation, or delivery races. Similar-looking assertions at these boundaries may be valuable. |
| `DeviceCommandTest` direct PENDING→EXECUTED/FAILED tests | Existing tests encode the unrestricted behavior flagged in B7.11. Rewrite against the agreed transition matrix when implementing that behavior fix; passing today does not validate the intended lifecycle. |

**T7.1 — Test maintenance follow-up (separate from behavior fixes):** inventory test→rule/boundary mappings, prune accessor-only and demonstrably duplicate cases in small batches, and keep validation/invariant coverage. Use targeted mutation checks or deliberately injected representative faults for disputed removals: if deleting a test loses the only detection of a relevant fault, retain or replace it. There is no defensible target reduction percentage from this review. Prioritize the missing ownership, claim-race, quota, stale-command, and event-order controls above over adding more constructor tests. No tests were removed in this audit.
