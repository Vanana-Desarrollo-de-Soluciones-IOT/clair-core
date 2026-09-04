# Domain Improvements — Analytics

Companion to `01-audit-report.md`. The audit report covers *packaging*; this document covers *meaning*: places where the analytics bounded context models the same concept two different ways, computes a number two different ways, or owns knowledge that belongs to another context. Each item states what the code does today (with the file), why it is incoherent, and a proposed domain-level change. Items are ordered by user-visible impact, not by effort.

Nothing here is required by the refactor branch plan; these are candidates for the product backlog once branches 0–5 have landed and analytics has a clean aggregate/port boundary to build on.

---

## 1. Defect: the AQI function returns HAZARDOUS for values that fall between breakpoints

**Today.** `AqiCalculationDomainServiceImpl.piecewiseLinear` (`analytics/domain/services/AqiCalculationDomainServiceImpl.java`) walks breakpoint ranges `[0.0–12.0], [12.1–35.4], …` and, if the concentration matches none, `return 500`. The ranges have gaps: `12.0 < c < 12.1`, `35.4 < c < 35.5`, `400 < c < 401`, `1000 < c < 1001`, etc. Integer sensor readings can never land in a gap, but **every caller passes averages** (`KpiLiveMetricsBuffer.computeAverages`, `SnapshotAggregationScheduler`, `DailyReportAggregationService`, `MetricsAggregationDomainServiceImpl`). A CO2 average of 400.5 ppm, well within normal indoor air, yields AQI 500 / `HAZARDOUS`.

**Why it matters.** This is a silent correctness bug in the number the whole product is built around. `AqiCalculationDomainServiceImplTest` exists but does not cover a gap value (it should; that is the control).

**Proposal.**
- Make breakpoints contiguous (`[0, 12.0], (12.0, 35.4], (35.4, 55.4] …`) and clamp above the last upper bound to 500 explicitly.
- Model the table as a value object `AqiBreakpointTable` with a `subIndex(double)` method and a unit test that sweeps 0..1000 in 0.01 steps asserting monotonicity. Monotonicity is the invariant; a gap breaks it.
- Introduce an `AqiStandard` enum/strategy (`EPA_PM25`, `WHO_2021`, a Peru `ECA` variant) so the table is data, not code, and so the product can later say which standard a report uses. Today the CO2 sub-index is a home-grown scale presented alongside an EPA-shaped PM2.5 scale with no label; the report should name it.

---

## 2. Two definitions of "average AQI"

**Today.**
- `SnapshotAggregationScheduler` computes the hourly AQI as *AQI of the mean concentrations* (`calculateAqi(avgPm25, avgCo2)`).
- `DailyReportAggregationService.DailyAccumulator.add` computes *mean of per-reading AQIs* (`aqiSum / count`) and separately counts categories per reading.
- `MetricsAggregationDomainServiceImpl.aggregate` first averages the per-device AQI values, then **overwrites** the result with the AQI of averaged concentrations (lines 800–806), so the `aqi` list it builds is dead work.
- `MonthlyAccumulator` weights daily `averageAqi` by `readingCount`, i.e. a mean of means-of-per-reading-AQIs.

**Why it matters.** AQI is non-linear; mean(AQI(x)) ≠ AQI(mean(x)). A day with half the readings at PM2.5 = 5 and half at 60 has AQI(mean=32.5) ≈ 94 (MODERATE) but mean(AQI) ≈ (21+153)/2 = 87. Trend deltas between the dashboard (snapshot-based) and the daily report (reading-based) will disagree by design, and users will notice.

**Proposal.** Pick one and name it in the ubiquitous language:
- **Exposure-weighted** (mean of per-reading AQI, plus time-in-category) is the more honest health metric and is what the daily report already does. Adopt it everywhere: the hourly snapshot stores `meanAqi` and `AqiCategoryBreakdown` too; the dashboard shows "AQI now" (latest reading window) and "AQI today" (exposure-weighted) as two distinct fields rather than a single ambiguous `aqiValue`.
- Encode the rule once in a domain service `AqiAggregationPolicy` and delete the ad-hoc accumulators.

---

## 3. Three pipelines read the same raw data with three different clocks

**Today.**

| Pipeline | Source | Window | Time zone | Trigger |
|---|---|---|---|---|
| Live KPI | in-memory `KpiLiveMetricsBuffer` | rolling 5 min by `recordedAt` | n/a | on event |
| Hourly snapshot | raw `telemetry_evaluations` via `EvaluationContextFacade.getHourlyTelemetryAggregation` (SQL) | `[HH:00, HH+1:00)` UTC | UTC | `@Scheduled` hourly |
| Daily summary | raw `telemetry_evaluations` via `JdbcTemplate` in analytics | calendar day | `America/Lima` | `@Scheduled` 00:15 Lima |
| Monthly summary | daily summaries | calendar month | `America/Lima` | `@Scheduled` 1st of month |
| KPI "historical" DAY/WEEK/MONTH | hourly snapshots | rolling `now − 1/7/30 days` | UTC | on request |

A user asking for "this month" gets a rolling 30 UTC days from the KPI endpoint and a Lima calendar month from the report endpoint. The daily job runs at 00:15 Lima but the hourly snapshot for 23:00–00:00 Lima is 04:00–05:00 UTC, so the two never line up on boundaries. The `MONTH` trend compares to the previous rolling 30 days; the monthly report compares to the previous calendar month.

**Why it matters.** Same word, different number. Also, the daily job reads raw telemetry across a context boundary with hand-written SQL that hard-codes evaluation's column names (`aq_co2`, `pm_pm2_5`) — a rename in evaluation breaks analytics silently (report C12).

**Proposal.**
- Analytics owns **one** materialised time series: `DeviceHourlyBucket` (device, hour start in UTC, count, sums, min/max, AQI breakdown). It is maintained **incrementally** from `TelemetryRecordedIntegrationEvent`, not by re-scanning evaluation's table. Daily and monthly summaries roll up from hourly buckets in the tenant's zone; the raw table is never read by analytics.
- A `ReportingCalendar` value object (zone + week start) belongs to the **organization** (device context) and is passed in the query. Hard-coding `America/Lima` in a `@Value` default is a tenant property leaking into a service.
- The KPI endpoint's `period` becomes `TrendPeriod` everywhere (report I18) with explicit semantics: `TODAY`, `THIS_WEEK`, `THIS_MONTH` (calendar, tenant zone) versus `LAST_24H`, `LAST_7D`, `LAST_30D` (rolling). Offer both, name both.

---

## 4. The live window is process memory

**Today.** `KpiLiveMetricsCache` is a Caffeine cache of `KpiLiveMetricsBuffer` objects (`expireAfterAccess(10 min)`). `AnalyticsSseService` holds `SseEmitter`s in a `ConcurrentHashMap`.

**Why it matters.**
- Restart the service and every device is "off or disconnected" for 5 minutes (`GET /live` → 404 `DeviceTelemetryUnavailableException(isLive=true)`).
- Run two instances behind a load balancer and each sees half the telemetry; the overview's `Freshness` flips between LIVE and STALE per request.
- Redis is already provisioned and used for caching and sessions.

**Proposal.** Promote the buffer to a domain concept `LiveTelemetryWindow` (device, duration, readings) behind a port `LiveTelemetryWindowRepository`; implement with a Redis sorted set keyed by device with score = `recordedAt` epoch and `ZREMRANGEBYSCORE` on write. SSE fan-out publishes through Redis pub/sub so any instance can serve the stream. The `Freshness` enum then derives from the newest score, which is the same number on every node.

---

## 5. Analytics decides device "freshness"; the device context decides device "presence"

**Today.**
- `Freshness { LIVE, STALE, NO_DATA }` in analytics is computed from whether the in-memory buffer has readings in the last 5 minutes.
- `DeviceStatus { OFFLINE, ONLINE, STANDBY, ERROR, … }` and `DeviceAssignment.lastSeenAt` in the device context are updated **only** by explicit edge presence events (`EdgePresenceController`). **Telemetry arrival does not touch `lastSeenAt`.** A device can be streaming readings every 10 seconds while the device context says `OFFLINE`, and vice-versa.
- Nothing in core ever marks a device `OFFLINE` on its own; there is no heartbeat timeout.

**Why it matters.** The dashboard tile ("LIVE") and the device list ("OFFLINE") can contradict each other for the same device at the same second. Two contexts own the same fact.

**Proposal.** Presence is a device-context concern; freshness of *data* is an analytics concern, and the second should be derived from the first, not computed independently.
- Device context subscribes to `TelemetryRecordedIntegrationEvent` → `assignment.markSeen(recordedAt)`; a `DevicePresenceMonitor` scheduled job marks assignments `OFFLINE` when `lastSeenAt` is older than the device type's heartbeat interval (a property of `DeviceType`, see 04-…-iot-ingestion.md §7).
- Analytics asks `DeviceContextFacade.getPresence(deviceIds)` and reports `dataFreshness` as `{ presence, lastReadingAt }`. Delete `Freshness`.

---

## 6. The overview endpoint is an N+1 fan-out over another context's object graph

**Today.** `OverviewDashboardQueryServiceImpl.handle` calls `DeviceContextFacade` for organizations → for each org, spaces → for each space, device ids → for each device, the live buffer or the latest snapshot; then a second pass for alert device/space names. It parallelises with `CompletableFuture.supplyAsync` on the common pool from inside a read-only transaction (report F3). `DeviceContextFacade` has grown to 15 methods to feed this one screen.

**Why it matters.** Analytics has to understand the org → space → device hierarchy to draw the overview; every new grouping (floor, building, tag) means another facade method and another loop. Latency scales with the number of spaces.

**Proposal.** Two options, in order of preference:
1. **Materialised `SpaceAnalyticsProjection` in analytics**, maintained from device-context integration events (`DeviceClaimedToSpace`, `DeviceUnassigned`, `SpaceRenamed`) plus the hourly bucket roll-up. The overview becomes one query on analytics' own tables. The facade shrinks to identity checks. This is the standard CQRS answer and is what "analytics" as a context is for.
2. If event-driven projection is too much for now, a single facade call `getOwnerTopology(ownerId) → OrganizationTopology(orgs → spaces → deviceIds)` returning the whole tree in one round-trip, and a single repository query `findLatestBucketsForDevices(deviceIds)`. Same result, two calls instead of O(spaces).

Either way, remove `CompletableFuture` from a `@Transactional` method.

---

## 7. Trend deltas are computed three ways

**Today.**
- LIVE dashboard: current 5-min average vs **latest hourly snapshot** (or vs itself → delta 0 if no snapshot). `KpiDashboardMetricsQueryServiceImpl:57-60`.
- Historical dashboard: current window vs **previous window of equal length**; `null` if no previous data.
- Daily/monthly report: `aqiDeltaPct` vs the **previous day/month's `averageAqi`**, only when the previous is `> 0`.
- `TrendAnalysisDomainService` rounds to 2 decimals; `MetricsAggregationDomainServiceImpl.round1` rounds to 1; `KpiDashboardMetrics` compact constructor rounds averages to 2; `AggregatedMetrics` receives 1-decimal values. The overview shows 1 decimal, the device page 2.

**Proposal.** One `MetricTrend` value object with an explicit `comparisonBasis` (`PREVIOUS_WINDOW`, `PREVIOUS_CALENDAR_PERIOD`, `LAST_SNAPSHOT`) so the UI can label it, one rounding rule in the transform layer (not in domain records), and `null`—never a fake `0`—when there is no basis.

---

## 8. Alert thresholds are stored in the device context but evaluated in alerting

**Today.** A threshold is a `DeviceMetricThresholdConfiguration` serialised to JSON and stored under the key `threshold.<METRIC>` in `DeviceAssignment.configuration: Map<String,String>` (`DeviceThresholdCommandServiceImpl`). Alerting fetches them via `ThresholdContextFacade`, maps `device.MetricThreshold` → `alerting.MetricType` by `valueOf(name())`, and evaluates. A malformed JSON value silently disables the rule (report F8). Analytics reports never mention thresholds or alert counts even though `DailyAlertCount` exists in alerting.

**Why it matters.** The *rule* ("alert me when PM2.5 > 35 for this device") is alerting's core concept, yet alerting cannot query, list, or version its own rules; it reads them out of another context's key-value bag. Two enums represent the same four metrics.

**Proposal.** Move the rule to alerting as an `AlertRule` aggregate (`deviceId`, `metric`, `comparator`, `threshold`, `enabled`, optionally `minDurationSeconds` for hysteresis and `cooldownSeconds`). Device context stops knowing about thresholds; `ThresholdContextFacade` is deleted. Analytics reports gain `alertsOpened`, `alertsResolved`, `minutesAboveThreshold` per metric by subscribing to `AlertIncidentChangedIntegrationEvent` and rolling into the hourly bucket.

---

## 9. Read models mislabelled as value objects, and a premium check in the wrong place

- `KpiDashboardMetrics`, `OverviewDashboardSnapshot`, `AggregatedMetrics`, `DeviceMetricsSnapshot`, `KpiTrendPoint` under `domain/model/valueobjects` are query results, not values an aggregate holds. Move to `domain/model/readmodels` (or `application/queryservices/results`) so the "valueobjects" folder means one thing.
- `ReportController.getMonthlyReport` enforces "premium only" by calling `ExternalBillingService.canAccessMonthlyReports`. A pricing rule in a controller cannot be unit-tested without MVC and is invisible to the query service. Make `MonthlyReportQueryService` apply a `ReportAccessPolicy` (port to billing) and throw `AccessDeniedException` itself; the controller only maps it.
- `DeviceTelemetryUnavailableException` is thrown for an empty result (report F2). Return `Optional.empty()` from the query service and let the controller produce 404 with a body that says `NO_LIVE_DATA` vs `NO_DATA_IN_PERIOD`. Exceptions for expected empties also make the analytics SSE `handleAsyncRequestNotUsable` workaround more likely to fire.

---

## 10. Feature candidates that follow from a coherent model

Once 1–8 are in place these become small additions rather than new pipelines. Listed because they are the kind of value an IoT air-quality product is expected to show and the current model cannot express.

| Feature | Domain addition | Depends on |
|---|---|---|
| **Time-in-category** ("your office spent 3 h 20 min in UNHEALTHY today") | `AqiCategoryBreakdown` already counts readings; add expected sampling interval from `DeviceType` and convert counts to duration. | §2, §5 |
| **Data completeness** ("94 % of expected readings received") | `expectedReadings = windowSeconds / deviceType.samplingIntervalSeconds`; store `completenessPct` on the bucket. Distinguishes "air was good" from "sensor was silent". | §3, §7 of ingestion doc |
| **Ventilation insight** from CO2 decay | Domain service fitting the CO2 decay slope after occupancy drops → `AirChangesPerHour` VO. Needs raw per-reading series, i.e. the live window or hourly bucket with min/max timestamps. | §4 |
| **Space and organization reports** (not only per device) | Roll up hourly buckets by the space projection. | §6 |
| **Peak-hour profile** (average AQI by hour-of-day over 30 days) | 24-slot histogram on the space projection. | §3, §6 |
| **Alert-aware reports** | `alertsOpened`, `minutesAboveThreshold` on the bucket. | §8 |
| **Comparative baseline** ("12 % better than last month", "worse than 70 % of similar spaces") | `MetricTrend.comparisonBasis`; anonymised fleet percentile as a scheduled projection. | §7 |
| **Standard-labelled AQI** (EPA vs WHO) | `AqiStandard` strategy; report carries the standard's name. | §1 |
| **Retention & downsampling policy** | Raw readings older than N days deleted; hourly buckets kept for a year; daily forever. A `RetentionPolicy` per plan type (billing facade). Today the raw table grows unbounded with one index. | §3 |

---

## Suggested order

1. §1 (bug, one file, one test) — do it in the audit-baseline branch as a `fix:` commit, it does not touch structure.
2. §2 + §7 (single AQI/trend definition) — domain services only.
3. §4 (Redis live window) — infrastructure swap behind a new port; unblocks multi-instance.
4. §5 + §8 (presence and rules move to their owning contexts) — cross-context, after branch 10 of the refactor plan.
5. §3 + §6 (owned hourly bucket, space projection) — the real CQRS read side; after branches 2–5 so the aggregate/port pattern exists to copy.
6. §10 features as product demand dictates.
