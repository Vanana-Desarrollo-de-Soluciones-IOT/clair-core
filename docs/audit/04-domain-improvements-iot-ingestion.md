# Domain Improvements — IoT Device Ingestion

Companion to `01-audit-report.md` and `03-domain-improvements-analytics.md`. Covers the path from a sensor reading on the edge to a stored, trusted, attributable record in core, and the surrounding device lifecycle (identity, presence, commands, rules). As with the analytics document, each item states what the code does today with the file, why it is incoherent, and a domain-level proposal.

Context for the reader: `docs/plans/00-overview.md` records that Kafka was removed and that the edge → core telemetry path was "broken, no telemetry arrives". The HTTP endpoints now exist (`/api/v1/evaluations/telemetry`, `/telemetry/batch`, `/api/v1/edge/*`), so this document assumes telemetry *does* flow and asks whether the model it flows into is sound.

---

## 1. Security: readings are accepted from anyone who can guess a hardware id

**Today.** `POST /api/v1/evaluations/telemetry` is `permitAll` (`iam/infrastructure/config/SecurityConfiguration.java:80`) and is *not* covered by `ServiceTokenAuthenticationFilter` (`shared/infrastructure/security/ServiceTokenAuthenticationFilter.java` guards only `/api/v1/edge/**` and `/telemetry/batch`). `TelemetryEvaluationController.evaluateTelemetry` resolves the device by hardware id or UUID and stores the reading. No header, key, or signature is checked. `HardwareId` is `CLAIR-` plus **4** uppercase alphanumerics (`device/domain/model/valueobjects/HardwareId.java`): 1.68 million possibilities, enumerable in minutes. `Device.apiKey` (32 random bytes, `ApiKey.generate()`) exists, is stored, and is handed to the edge in the roster, but is **never verified on ingestion**. `/api/v1/devices/presence/events` is likewise unauthenticated via its own `@Order(0)` chain (`DevicePresenceSecurityConfiguration`).

**Impact.** Forged readings → forged alerts → push notifications and e-mails to real users; forged presence → devices shown offline. Also poisons every analytics aggregate.

**Proposal.** Two authentication planes, each a domain concept:
- **Service plane** (edge ↔ core): the existing shared secret in `X-Core-Token`. Apply to *every* edge-facing route including single-record telemetry, or delete the single-record route and keep only `/edge/telemetry/batch`.
- **Device plane**: each reading carries the device's `ApiKey` (header `X-Device-Key` or an HMAC of the body with the key). Introduce a `DeviceCredentials` concept in the device context (`apiKey`, `issuedAt`, `rotatedAt`, `revoked`) and a `DeviceContextFacade.authenticate(hardwareId, apiKey) → Optional<DeviceId>` used by an `DeviceAuthenticationFilter`. The controller then receives an already-resolved `DeviceId` as a request attribute and the `resolveDeviceId` UUID-or-hardware-id heuristic disappears. Lengthen `HardwareId` or stop treating it as a secret; it is a label.

This is branch `0b` of `02-refactor-branch-plan.md`. It is listed here because the *domain* fix (device credentials as a first-class concept) is more than a filter.

---

## 2. Two ingestion contracts for the same reading

**Today.**

| Route | Shape | Fields | Validation | Batch |
|---|---|---|---|---|
| `POST /telemetry` | nested camelCase record `EvaluateTelemetryRequest` (`airQuality{co2,…}`, `particulateMatter{pm1_0,…}`, `connectivity{…}`, `location{country}`) | `timestamp` = `LocalTime` string, `uptime` = **string**, `created_at` optional ISO instant | Bean Validation | no |
| `POST /telemetry/batch` | flat snake_case `JsonNode` (`device_id`, `co2`, `pm2_5`, `wifi_status`, `recorded_at`, `occurred_at`, `client_ref`) | `device_time` may be an Instant **or** a LocalTime (`batchRequest` tries both) | hand-rolled `requireText/requireNumber/requireInteger` over 14 names | ≤ 10 records, each re-wrapped into `EvaluateTelemetryRequest` and pushed through the single-record handler → N transactions, N events |

`TelemetryRecordedIntegrationEvent` records exist in alerting, analytics and evaluation with an 18-field snake-ish shape that matches neither controller and have 0 consumers.

**Why it matters.** The edge team has to support two payloads; validation rules drift (single path accepts `created_at`, batch requires `recorded_at` *and* `occurred_at`); a batch of 10 is not a batch; error reporting differs (400 vs per-record `VALIDATION_ERROR`).

**Proposal.** One canonical `TelemetryReading` contract owned by the ingestion context and published in OpenAPI under an "Edge" group:

```
POST /api/v1/edge/telemetry
{ "readings": [ { "reading_id": "<uuid, edge-generated>", "hardware_id": "...",
                  "measured_at": "<instant>", "received_by_edge_at": "<instant>",
                  "measurements": { "co2_ppm": 612.4, "pm1_0_ugm3": 4, "pm2_5_ugm3": 11, "pm10_ugm3": 18,
                                    "temperature_c": 23.1, "humidity_pct": 48.0 },
                  "device": { "uptime_s": 3600, "health_pct": 100, "firmware": "1.4.2",
                              "wifi": { "status": "connected", "ssid": "...", "rssi_dbm": -61 } } } ] }
→ 207 Multi-Status: [ { "reading_id": "...", "status": "STORED|DUPLICATE|REJECTED", "reason": "..." } ]
```

One typed request record with Bean Validation; a `TelemetryReadingCommandFromResourceAssembler`; batch size 500; a single `IngestTelemetryBatchCommand`. Delete the single-record route (the edge already has an outbox and batches naturally) or make it a one-element batch.

---

## 3. No idempotency, so edge retries create duplicate readings

**Today.** The edge repo's outbox retries with backoff (`docs/plans/00-overview.md` item 4). Core has no unique key on `telemetry_evaluations` other than the surrogate id and no dedupe on `(device_id, recorded_at)` or on `client_ref`. A retried batch after a timeout stores every reading twice. Alerting is protected by its "one open alert per device+metric" rule; analytics is not: averages are unaffected but `readingCount`, category counts and completeness (once added) double.

**Proposal.** `TelemetryReading` aggregate identity = `ReadingId` supplied by the edge (`reading_id` above), persisted with a unique constraint; on conflict the response says `DUPLICATE`, the event is **not** re-published. Fallback key when the edge cannot supply one: `(deviceId, measuredAt)` unique. This is the standard at-least-once + idempotent-consumer pairing and it is the only thing that makes the edge's retry loop safe.

---

## 4. The "evaluation" context does not evaluate anything

**Today.** `TelemetryEvaluation` (`evaluation/domain/model/entities/TelemetryEvaluation.java`) stores the raw reading plus two fields that *look* computed but are copied from the request: `status: String` (e.g. `"Optimal"`) and `healthStatus: Integer` (0–100). Both come from the device firmware. `EvaluateTelemetryCommand` has 10 invariant checks that duplicate the aggregate constructor's 10 checks verbatim. Alerting evaluates thresholds; analytics evaluates AQI; evaluation evaluates nothing.

**Why it matters.** The name promises a domain behaviour that lives elsewhere, so new developers look in the wrong context for AQI or threshold logic. `status: "Optimal"` is an unvalidated free string from the device that the API surfaces as if core decided it.

**Proposal.** Rename the context to **`telemetry`** (or `ingestion`) with aggregate `TelemetryReading`. Its responsibilities are exactly: authenticate source, validate plausibility (§5), deduplicate (§3), persist, publish `TelemetryRecordedIntegrationEvent`. Device-reported `status`/`healthStatus` become `DeviceSelfReport` (a VO on the reading, or better, routed to the device context as `DeviceHealthReported` to update `DeviceAssignment`). If a real "evaluation" step is wanted later (e.g., classify each reading GOOD/MODERATE at ingest time), it becomes a domain service *in analytics*, which owns the AQI standard.

---

## 5. No plausibility validation, no quality flag, no sensor semantics

**Today.** `AirQuality` requires non-null `Double`s; `ParticulateMatter` requires non-null **`Integer`s**; nothing bounds them. `co2 = -40`, `temperature = 850`, `humidity = 140`, `pm2_5 = 2147483647` are all stored and averaged. `Connectivity.signalStrength` *is* range-checked (−150..0), so the team knows how; it was just not done for the measurements. PM values as integers lose the 0.1 µg/m³ resolution that PMS5003-class sensors report, and analytics then averages them as doubles anyway.

**Proposal.**
- A `Measurement<Unit>` VO family (`Concentration(ppm)`, `Concentration(µg/m³)`, `Temperature(°C)`, `RelativeHumidity(%)`) each with a **physical** range (hard reject: CO2 < 0 or > 40 000, RH outside 0–100) and a **sensor** range (soft flag: outside the `DeviceType`'s spec, e.g. SCD40 CO2 400–5000).
- `ReadingQuality { VALID, SUSPECT, INVALID }` on the aggregate; `INVALID` is stored for forensics but never published to analytics/alerting; `SUSPECT` is published with the flag so analytics can exclude it from AQI and alerting can require two consecutive SUSPECT readings before firing.
- Store PM as `Double` (or `BigDecimal(1)`) end-to-end. Today alerting converts to `BigDecimal.valueOf(double)`, analytics uses `double`, ingestion uses `Integer`: three numeric types for one quantity.

---

## 6. Time is modelled with the wrong types and duplicated fields

**Today.** `TelemetryEvaluation` stores `deviceTime: LocalTime` (device wall-clock *time of day*, no date, no zone, `nullable = false`), `recordedAt: Instant` (taken from the optional `created_at` request field, else `Instant.now()` at core), and `auditFields.createdAt` (insertion time). The batch route accepts `device_time` as either an Instant or a LocalTime and truncates the former to a `LocalTime` in UTC. `TelemetryRecordedEvent` carries `occurredAt` and `recordedAt` **both set to `recordedAt`**, and `deviceTime` as a `String`.

**Why it matters.** Analytics buckets by `recorded_at`, which is core receipt time when the edge omits `created_at`, so a 3-hour edge outage followed by an outbox flush files three hours of readings into the current hour. `LocalTime` without a date is unusable for anything and can never be reconciled with `recordedAt`.

**Proposal.** Three instants, all `Instant`, all explicit, none optional:
- `measuredAt` — the sensor's own clock (edge converts device time to UTC using the device's reported uptime and the edge's clock; if the device has no RTC, the edge stamps it).
- `receivedByEdgeAt` — for drift detection.
- `receivedByCoreAt` — audit.
Analytics buckets by `measuredAt`; a `ClockDrift` VO (`measuredAt − receivedByEdgeAt`) beyond a threshold marks the reading `SUSPECT` (§5). Drop `deviceTime` and `uptime`-as-string; `uptimeSeconds: Duration` stays as device health data.

---

## 7. `DeviceType` is a string; it should be the capability model

**Today.** `DeviceType(String value)`, always `"air-quality-v1"` (seeded in `DeviceCommandServiceImpl.handle(SeedDevicesCommand)`). Nothing reads it. Consequently core cannot know a device's sampling interval (needed for presence timeout and data completeness), which measurements it *should* send (needed to detect a dead sensor), its firmware, or its sensor ranges (§5).

**Proposal.** `DeviceModel` aggregate (or a rich VO catalogue) in the device context: `modelId`, `expectedMeasurements: Set<MeasurementKind>`, `samplingInterval: Duration`, `heartbeatTimeout: Duration`, `sensorSpecs: Map<MeasurementKind, Range>`, `supportedCommands: Set<DeviceCommandType>`. `Device` references it by id. The roster sends it to the edge so the edge can validate locally too. `firmwareVersion` becomes a field on `Device`, updated from the reading's `device.firmware` (§2) via a `DeviceSelfReported` domain event.

---

## 8. Presence is push-only from the edge and disconnected from telemetry

**Today.** `DeviceAssignment.status` and `lastSeenAt` change only when the edge posts to `/api/v1/edge/presence` (`EdgePresenceController` → `DevicePresenceCommandServiceImpl`) or when a command is acknowledged. **Telemetry arrival does not update `lastSeenAt`** (no caller of `markLastSeen`/`markOnline` in the telemetry path). Core never times a device out. `DeviceStatus` mixes connectivity (`ONLINE/OFFLINE`), power mode (`STANDBY`), fault (`ERROR`) and lifecycle (`MAINTENANCE`, `DECOMMISSIONED`) in one enum; `markStandby()` overwrites `ONLINE`, so a device in standby that is happily heartbeating is "not online".

**Proposal.** Split into three orthogonal facts on `DeviceAssignment`:
- `connectivity: ConnectivityState { CONNECTED, DISCONNECTED }` derived by core from `lastSeenAt` + `DeviceModel.heartbeatTimeout` via a scheduled `DetectSilentDevicesCommand`. Edge presence events become *hints* that advance `lastSeenAt`, not the source of truth.
- `powerMode: PowerMode { ACTIVE, STANDBY }` changed only by acknowledged commands.
- `lifecycle: LifecycleState { PROVISIONED, CLAIMED, ACTIVE, MAINTENANCE, DECOMMISSIONED }` changed only by user/admin commands.
Telemetry arrival → `assignment.recordActivity(measuredAt)` via the device context's own listener on `TelemetryRecordedIntegrationEvent`. Analytics then asks presence from the device facade instead of inventing `Freshness` (analytics doc §5). Emit `DeviceWentSilent` / `DeviceCameBack` integration events; notifications already has a delivery channel for them.

---

## 9. Alert rules live in the device context as JSON strings in a map

**Today.** `DeviceAssignment.configuration: Map<String,String>` (`@ElementCollection`) holds `threshold.PM25 → {"metric":"PM25","value":35,"enabled":true}` serialised with Jackson by `DeviceThresholdCommandServiceImpl`. `ThresholdContextFacadeImpl` deserialises with `ObjectMapper` and returns `Optional.empty()` on any exception (report F8). Two enums (`device.MetricThreshold`, `alerting.MetricType`) describe the same four metrics and are bridged by `MetricType.valueOf(threshold.metric().name())`, which throws `IllegalArgumentException` the day one enum gains a value the other lacks.

**Why it matters.** The alerting context cannot list, audit, version or index its own rules; the device aggregate carries a serialised foreign concept and its persistence assembler (refactor branch 5) must round-trip a map whose values it does not understand. A corrupted entry silently disables alerting.

**Proposal.** `AlertRule` aggregate in **alerting**: `ruleId`, `deviceId` (or `spaceId` for space-wide rules), `metric`, `comparator {ABOVE, BELOW}`, `threshold`, `enabled`, `minDuration` (hysteresis: fire only after N seconds above), `cooldown`, `severityBands`. Commands `DefineAlertRule`, `EnableAlertRule`, `RemoveAlertRule` replace `WriteDeviceThresholdCommand`/`RemoveDeviceThresholdCommand`. The device context loses `configuration` entirely unless something else needs it (nothing does today). `ThresholdContextFacade` is deleted. One `MeasurementKind` enum in `shared/domain` replaces `MetricThreshold`/`MetricType`.

---

## 10. Command lifecycle has no expiry and two acknowledgement paths

**Today.** `DeviceCommand` moves `PENDING → SENT` when the edge claims it (`EdgeCommandAcknowledgementService.claimForEdge`, 300 s lease with re-delivery) and `SENT → EXECUTED|FAILED` on ack. There is no `EXPIRED`/`CANCELLED`: a command for a device that never comes back stays `SENT` and is re-delivered every poll forever. Two ack entry points exist — the edge one and `DeviceControlCommandServiceImpl.handle(AcknowledgeDeviceCommandCommand)` for the user API — both containing the same `switch(type) → assignment.markStandby()/markOnline()` side-effect. `DispatchPendingDeviceCommandsCommand` marks commands `SENT` with no transport at all (a leftover from the Kafka design; its `permitAll` route no longer has a controller, report J6). Edge responses are hand-built `Map<String,Object>`.

**Proposal.**
- Add `expiresAt` (from `DeviceModel` or per-command TTL) and states `EXPIRED`, `CANCELLED`; a `ExpireStaleCommandsCommand` scheduled job; the claim query excludes expired.
- One `acknowledge(result, detail, source)` behaviour on the aggregate; the power-mode side effect becomes a domain event `DeviceCommandExecuted` handled once in the device context (feeds §8's `powerMode`), not duplicated in two services.
- Delete `DispatchPendingDeviceCommandsCommand`.
- Typed `EdgeCommandResource` / `EdgeAlertResource` records for the edge API (refactor branch 8).

---

## 11. Provisioning: fake devices on every boot and limits in two places

**Today.** `DeviceSeedOnStartup` runs `SeedDevicesCommand(5)` on every `ApplicationReadyEvent` in every environment, creating `SN-0001..SN-0005` with random hardware ids and notifying the edge. `Organization.getMaxSpaces()`/`getMaxDevices()` return hard-coded 5/10 while `ExternalBillingService.getMaxSpaces/getMaxDevices` fetches the same limits from billing by plan. `PairDeviceCommand` (create assignment + claim token) and `ClaimDeviceCommand` (bind to space/owner) are two steps with a `ClaimToken` in between — that is a sound design; the seeder just bypasses the factory-inventory story it implies.

**Proposal.**
- Move seeding behind a `demo` profile or a `POST /api/v1/admin/devices/import` command fed by a manufacturing CSV (`serial, hardware_id, model, api_key_hash`). Real inventory arrives from the factory, not from a loop.
- Remove `getMaxSpaces/getMaxDevices` from `Organization`; the quota is a billing `PlanQuota` VO obtained through the ACL and enforced in the device command services (they already call billing for organization count).
- Store `apiKey` hashed (it is a credential, §1); the roster sends it to the edge once at claim time, or the edge derives per-device HMAC keys.

---

## 12. Storage and retention for a time series

**Today.** `telemetry_evaluations` is a plain table with one composite index `(device_id, recorded_at)`, no partitioning, no retention. Every reading is one row of ~15 columns; at one reading per 10 s per device that is 8 640 rows/device/day. Analytics reads it back with `SELECT … WHERE recorded_at BETWEEN` full scans per hour and per day.

**Proposal** (infrastructure, but it constrains the domain):
- Partition by month (`PARTITION BY RANGE (measured_at)`) or use a TimescaleDB hypertable; either is invisible to JPA behind the repository adapter once branch 4 exists.
- `RetentionPolicy` per plan (`FREEMIUM`: raw 30 days, `PREMIUM`: raw 365 days) executed by a scheduled `PurgeExpiredReadingsCommand`; hourly buckets (analytics doc §3) are the long-term record.
- Adopt Flyway now (refactor branch 11) so the partitioning DDL is a migration, not a `@PostConstruct`.

---

## 13. Smaller incoherences worth fixing while nearby

| Where | What | Fix |
|---|---|---|
| `TelemetryEvaluationCommandServiceImpl.handle` l.310 | `TelemetryRecordedEvent.pm100` is assigned `particulateMatter().pm1_0()` — the field named PM100 receives PM1.0. Nobody reads `pm100`, so it is latent, but the next consumer will get the wrong number. | Replace the 18-field event with `TelemetryRecordedIntegrationEvent(readingId, deviceId, measuredAt, measurements: Map<MeasurementKind, Double>, quality)`. |
| same, l.302 | `hardwareId` always `null` in the event. | Remove the field or populate it. |
| same, l.315 | `healthStatus` (Integer) → `toString()` → String field. | Typed. |
| `EvaluateTelemetryCommand` | 10 checks duplicated from the aggregate constructor. | Commands validate *shape* (non-null); aggregates validate *invariants*. Delete one copy. |
| `TelemetryEvaluationRepository.findLatestByDeviceId` | Unused duplicate of `findByDeviceId`. | Delete. |
| `EvaluateTelemetryRequest.uptime` | Documented as `"00:00:20"` in `@Schema(example)` but parsed with `Long.parseLong`. The example would be rejected. | Fix the example or accept both. |
| `EdgeAlertController.pending`, `EdgeCommandController.pending` | `since` parsed with `Instant.parse` without try/catch → 500 on bad input (the roster controller does it right). | Shared `EdgeCursor` VO parsing both epoch-ms and ISO. |
| `DeviceEdgeController` vs `EdgePresenceController` | Two presence endpoints, one unauthenticated. | Keep `/api/v1/edge/presence` only. |
| `AlertRepository.findPendingForEdge` | JPQL joins alerting's `Alert` with device's `Device` to fetch `hardwareId`. | Alerting stores `hardwareId` on the alert at creation (it already stores `deviceName`/`spaceName` denormalised) or asks the facade in batch. |

---

## Suggested order

1. §1 — security; ship with refactor branch `0b`, before anything else.
2. §13 row 1–3 and §3 — event shape + idempotency. Small, unblocks the edge team's retry story.
3. §2 + §6 — one contract, three instants. Coordinate with the edge repo; version the route (`/api/v1/edge/telemetry` new, old routes deprecated for one release).
4. §4 + §5 + §7 — rename to `telemetry`, add `Measurement` VOs, `ReadingQuality`, `DeviceModel`. Best done as the refactor's branch 2b since the aggregate is being re-cut anyway.
5. §8 + §10 — presence and command lifecycle; after refactor branch 10 (events after commit).
6. §9 — `AlertRule` in alerting; after branch 10.
7. §11, §12 — provisioning and storage; independent, schedule by ops need.
