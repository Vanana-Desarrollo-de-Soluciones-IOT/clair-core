# clair-core — Behaviour Backlog

Everything here changes what the system does. None of it is part of the structural refactor in `01-audit-report.md`, and none of it may share a branch with a structural phase, because a structural phase is verified by "DDL unchanged, tests unchanged" and behaviour work breaks both gates by design.

## Rule

An item may start only when the structural phase it names is merged. Each item is one branch, one `fix:` or `feat:` commit series, with the test named in its row written **first** as the control.

## Before Phase 0 (independent of structure)

| # | Item | Source | Control |
|---|---|---|---|
| B0.1 | Delete `DeviceEdgeController` and `DevicePresenceSecurityConfiguration`; keep `/api/v1/edge/presence`. Add `/api/v1/evaluations/telemetry` to `ServiceTokenAuthenticationFilter.PATHS`. | I§1 | request without token → 401 |
| B0.2 | AQI breakpoint gaps: make ranges contiguous, clamp above the last bound. | A§1 | `AqiCalculationDomainServiceImplTest` sweeps 0..1000 step 0.01, asserts monotonic |
| B0.3 | `TelemetryRecordedEvent.pm100` assigned from `pm1_0`; `hardwareId` always null; `healthStatus.toString()`. | I§13 rows 1–3 | event field test |

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