# MVP telemetry and analytics changes

Date: 2026-09-07. Scope: a small, single-instance indoor-air MVP with no planned fleet expansion.
This report supersedes the broader analytics/ingestion roadmap where the scopes differ.
Ventilation-rate estimation and exposure-dose metrics are useful, but explicitly **post-MVP**.

## Causes and implemented changes

| Cause | Change | Result |
|---|---|---|
| Fractional concentrations between breakpoints fell through to AQI 500. Integer PM samples also produced fractional hourly/live averages. | PM-only calculator; truncate PM to one decimal, use revised EPA breakpoints, round the resulting index, explicitly cap the display at 500. | PM 8 yields 44; CO₂ 400.5 cannot poison the index. Invalid/missing PM has no calculated index. |
| CO₂ was treated as an AQI pollutant. | Remove the CO₂ sub-index; retain measured CO₂ and its existing alert evaluation separately. | High CO₂ remains actionable without changing the PM index. |
| Instantaneous/recent averages were labelled as regulatory AQI. | Analytics responses expose `indexLabel`: `Indicative PM2.5 index (EPA breakpoints; not NowCast)`. Existing numeric field names remain for client compatibility. | Clients can accurately label live, hourly, daily and monthly values without a NowCast implementation. |
| Network retries appended rows and published duplicate events. | Required Edge UUID; atomic PostgreSQL insert-on-conflict with uniqueness on `(device_id, reading_id)`. | Exact retries return the existing row; one event per new reading. Reusing an ID with changed payload is rejected. |
| LocalTime lacked date/offset; batch `occurred_at` was discarded. | Replace LocalTime in the application contract with reading identity; require a full measurement instant. Store batch `occurred_at` in existing `recorded_at`. Core insertion time remains `created_at`. | Late readings are aggregated by measurement time; responses explicitly expose `measuredAt` and `receivedAt`. |
| Integer PM discarded precision; sensor values were only null-checked. | Double PM through REST, domain, persistence and events; finite/range validation. | Decimal PM reaches analytics and alerts. Invalid new readings are rejected before persistence/publication. |
| Daily reports averaged indices; other paths indexed concentrations; historical dashboards averaged hourly means. | Index the mean PM concentration for each requested period. Historical dashboards query raw concentration aggregates through the evaluation facade. Monthly means remain sample-count weighted. | The same device and exact time window use the same concentration/index rule. Overview devices retain equal weighting. |
| Empty/racing live buffers could produce zero or inconsistent averages. | Optional empty result; one locked snapshot for count, sums and measurement time; measurement-ordered queue for expiry. | No fake zero reading; late arrivals expire correctly; last live timestamp reflects the measurement. |
| Reruns skipped stored reports. | Upsert daily/monthly summaries by their natural key, refresh the next period's comparison, refresh the month after daily rebuilds. Rebuilt hourly snapshots replace existing buckets and consolidate old duplicate buckets. | Late-data and formula corrections can repair derived reports while preserving raw measurements. |
| Nested common-pool tasks split the overview transaction across threads. | Synchronous organization/space traversal; keep existing bulk snapshot retrieval. | Simpler single-instance execution with ordinary transaction boundaries. |

EPA defines PM₂.₅ calculation using concentration truncation to one decimal and pollutant-specific
breakpoints. The apparent decimal gaps are intentional after truncation; inventing continuous
half-open EPA bands would change that procedure. The Good ceiling is 9.0 µg/m³, and the upper bands
also differ from the original calculator. The 500 cap here is a product display limit; EPA guidance
allows extrapolation beyond 500. See [EPA technical guidance, calculation procedure and Table 6](https://document.airnow.gov/technical-assistance-document-for-the-reporting-of-daily-air-quailty.pdf).

Recent, daily and monthly means have different time windows and need not agree. None of these
changes supplies regulatory completeness rules or NowCast. See [AirNow's explanation of daily and current AQI](https://www.airnow.gov/aqi/aqi-basics/using-air-quality-index/).

## Schema and migration

`V3__telemetry_identity_and_precision.sql` is additive to V1/V2; applied migration files were not edited.

- Add non-null UUID `telemetry_evaluations.reading_id` and unique `(device_id, reading_id)` constraint.
- Give historical rows `reading_id = id`. This preserves every row; it does not retrospectively identify duplicates.
- Convert `pm_pm1_0`, `pm_pm2_5`, `pm_pm10` from integer to PostgreSQL `double precision`.
- Make old `device_time` nullable. Its historical values stay available for inspection; new code neither reads nor writes it.
- Reuse `recorded_at` (measurement instant) and `created_at` (Core insertion/receipt instant), both existing timestamp-with-time-zone columns. No third Edge receipt timestamp is required.

A previously discarded measurement date cannot be reconstructed. Existing `recorded_at` values are
preserved as the best available historical measurement times. Validation applies to new input;
raw SQL analytics also excludes historical rows outside the supported ranges below. Raw invalid
rows remain in storage; the stricter domain model can reject them on telemetry-detail reads, so
inspect legacy outliers before rollout rather than assuming old rows were validated.

See [migration operations](migrations.md) for V1/V2 adoption details and the V3 rollout sequence.

## Edge changes required

The routes and authentication stay the same. The **payload contract changes**: old senders without
reading IDs/full instants must be upgraded together with Core.

1. Generate a UUID once for each measurement and persist it with the outbound queue entry. Reuse it
   on every retry, including after an Edge reboot. Never generate the UUID inside the HTTP retry loop.
   Different measurements need different IDs even when their values/timestamps match.
2. Send the measurement's full ISO-8601 timestamp with `Z` or an offset. Synchronize the clock; never
   turn a local time such as `14:30:25` into today's date on Core. Unknown measurement time is a
   validation error, not a reason to substitute upload time. Core normalizes instants to microseconds.
3. Preserve decimals for all PM fields. JSON integers are still valid numbers; remove integer casts
   if the sensor reports decimal values. Hardware replacement is not required. If a gateway owns
   envelopes/retries, these envelope changes belong there; firmware only needs changing where it
   currently truncates PM or cannot provide the measurement time.
4. Retry the same immutable payload. Do not refresh uptime, connectivity, health, status or timestamp
   while reusing an ID. A changed payload under an existing identity returns a validation error.
5. Keep batch size at 10 or fewer. Partial success remains intentional. An infrastructure failure
   can occur after earlier records committed; retrying with the same IDs is safe.

Batch example, `POST /api/v1/evaluations/telemetry/batch`:

```json
{
  "records": [{
    "client_ref": "outbox-1842",
    "reading_id": "40e67f87-2c0a-47ef-a3ed-7999e106cc9c",
    "device_id": "CLAIR-0001",
    "occurred_at": "2026-09-07T14:30:25.123Z",
    "uptime_seconds": 3600,
    "co2": 400.5,
    "temperature": 22.5,
    "humidity": 45.0,
    "pm1_0": 3.2,
    "pm2_5": 8.05,
    "pm10": 12.4,
    "wifi_status": "ONLINE",
    "network_name": "room-wifi",
    "signal_strength": -50,
    "country": "PERU",
    "health_status": 100,
    "status": "STABLE"
  }]
}
```

`client_ref` is only response correlation, not deduplication identity. `device_time` and
`recorded_at` are no longer required in batch and do not override `occurred_at`.
For compatibility, successful first submissions and exact retries both acknowledge `CREATED`;
that acknowledgement means the record is stored, not that a new row was necessarily inserted.
The singular endpoint likewise returns the existing resource with HTTP 201 on an exact retry.

Singular example, `POST /api/v1/evaluations/telemetry`:

```json
{
  "deviceId": "CLAIR-0001",
  "readingId": "40e67f87-2c0a-47ef-a3ed-7999e106cc9c",
  "measuredAt": "2026-09-07T14:30:25.123Z",
  "uptime": "3600",
  "airQuality": {"co2": 400.5, "temperature": 22.5, "humidity": 45.0},
  "particulateMatter": {"pm1_0": 3.2, "pm2_5": 8.05, "pm10": 12.4},
  "connectivity": {"status": "ONLINE", "network": "room-wifi", "signalStrength": -50},
  "location": {"country": "PERU"},
  "healthStatus": 100,
  "status": "STABLE"
}
```

The singular endpoint accepts `reading_id` as an alias of `readingId`, and `occurred_at` or
`created_at` as aliases of `measuredAt`. Use one measurement-time field, not conflicting aliases.
Telemetry responses replace `deviceTime` with `readingId`, return decimal PM, and expose
`measuredAt`/`receivedAt` alongside the retained `recordedAt`/`createdAt` names.

Supported input bounds: CO₂ 0–1,000,000 ppm, temperature −50–100 °C, humidity 0–100%, each PM field
0–10,000 µg/m³, health 0–100, nonnegative integral uptime. All measurements must be finite.
Temperature/PM bounds are broad **MVP product bounds**, not a calibration claim for a specific
sensor. Live windows exclude readings older than five minutes and readings dated in the future.

## Deployment and report repair

1. Back up the database. Pause Edge uploads and the old Core instance; retain Edge queue contents.
2. Upgrade the Edge payload producer/queue. Assign stable IDs once to queued legacy entries where
   their full measurement instants are known. Do not replay already-accepted legacy rows under new
   IDs and assume Core can recognize them; historical deduplication is unavailable.
3. Start the new Core. Flyway applies V3 and Hibernate validates it. Resume upgraded Edge delivery.
4. Rebuild the historical date range whose old chart/report indices must be corrected. On a
   controlled startup, set both `CLAIRCORE_REPORTS_REBUILD_FROM=YYYY-MM-DD` and
   `CLAIRCORE_REPORTS_REBUILD_THROUGH=YYYY-MM-DD` (inclusive, closed days only). Keep the normal
   `CLAIRCORE_REPORTS_ZONE=America/Lima`. The runner rebuilds hourly snapshots, then daily reports
   chronologically; daily rebuilds refresh their months and the next period's comparisons.
5. Remove the rebuild variables after completion. Use the same bounded rebuild for late arrivals
   into an already summarized day. Run one rebuild/scheduler writer at a time; this MVP does not
   implement distributed job locking. Rebuilds are not automatically triggered per reading.
6. Update dashboard labels to consume `indexLabel`, keep CO₂ ppm separate, and display no-data
   states. Category shares are percentages of samples, not percentages of elapsed time.

An in-progress month's report may now exist after a daily rebuild; `daysCovered` and `readingCount`
show its coverage. They are not a completeness percentage. Sample means assume comparable sampling;
this release does not estimate exposure between irregular readings or across missing intervals.

## Scope deliberately deferred

Ventilation-rate/CO₂-decay estimation and exposure dose remain useful future metrics, but **neither
is MVP work**. Also deferred: time-in-category, learned baselines, humidity correction, cross-device
calibration, completeness percentages, DeviceModel/catalogue, quality-state workflow, Redis live
windows/SSE, incremental event-fed buckets, partitioning and per-tenant reporting zones.

Existing spaces are reused through device assignments. The overview keeps equal device weighting,
not sample-frequency weighting. Raw-day loading and organization/space query fan-out remain acceptable
for this fixed installation; no claim is made that this release scales to a large fleet.

## Validation

Controls cover EPA boundary values and a 0–1000 PM sweep at 0.01 increments, invalid sensor values,
measurement-time/decimal batch forwarding, empty/out-of-order live windows, and PM isolation from CO₂.
PostgreSQL tests cover V1–V3 fresh startup with Hibernate validation, legacy migration preservation,
concurrent duplicate delivery with one row/event, and report reruns without duplicate summaries.
Final validation: **608 tests passed, 0 failures, 0 errors, 0 skipped**, with the PostgreSQL tests enabled.
`git diff --check` also passed. No production database was modified.
