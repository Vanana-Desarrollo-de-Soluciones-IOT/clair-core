# Edge integration contract, v1

Frozen 2026-09-11 as the Phase 0 baseline of `IMPLEMENTATION_PLAN.md`. The JSON files next to this
document are the reference fixtures: they describe what `clair-core` actually does today, plus the
additive fields marked **proposed**. Core is authoritative; edge and embedded adapt to it.

Two hops, both plain HTTP:

| Hop | Direction | Auth | Fixtures |
|---|---|---|---|
| core ↔ edge | edge pulls and pushes; core sends a hint | `X-Core-Token` (edge → core), `X-Edge-Token` (core → edge) | `core-edge/` |
| device ↔ edge | device pulls and pushes only | `X-Hardware-Id` + `X-API-Key` | `device-edge/` |

## Core ↔ edge routes

| Route | Fixture | Notes |
|---|---|---|
| `GET /api/v1/edge/devices?since&afterId&limit` | `roster.*.response.json` | `since` is epoch ms or ISO-8601. `watermark` is the last row handed back, never wall-clock. `deleted: true` rows are tombstones the edge must keep. Empty page keeps the caller's `since`. `assignment_id` is the current pairing generation (null while unclaimed); when it changes, the edge drops every cached command carrying a different one. |
| `POST /api/v1/evaluations/telemetry/batch` | `telemetry.batch.*.json` | At most 10 records. HTTP 200 with per-record results; inspect each. `client_ref` is echo only. `device_id` may be the core UUID or the hardware id. |
| `GET /api/v1/edge/commands/pending?hardware_id&since&limit` | `commands.pending.response.json` | Claiming: returned commands move `PENDING → SENT` under a 300 s lease. A lost response is redelivered after the lease. Each command carries the `assignment_id` it was issued under; unlinking a device expires its outstanding commands, and an ack for a stale generation returns 409 and voids the command. |
| `POST /api/v1/edge/commands/{commandId}/ack` | `commands.ack.*.request.json` | 200 OK, 409 already terminal, 404 unknown or not owned. `result` is `OK` or `FAILED`. |
| `GET /api/v1/edge/alerts/pending?since&limit` | `alerts.pending.response.json` | Returns `ACTIVE` and `RESOLVED` alerts ordered by `occurred_at`. See the ACK section: this is not a delivery queue yet. |
| `POST /api/v1/edge/alerts/{alertId}/ack` | `alerts.ack.request.json` | **Business acknowledgement**: moves `ACTIVE → ACKNOWLEDGED`. 409 if already ACKNOWLEDGED or RESOLVED. |
| `POST /api/v1/edge/presence` | `presence.request.json` | `device_id` is the core UUID. `status` is a `DeviceStatus` name. |
| `POST {edge}/api/v1/edge/notify` | `notify.request.json` | Core → edge hint. `resource` ∈ `device`, `command`, `alert`. Carries no data; edge must still poll on a timer. |

Errors: `error.validation.response.json` for bean-validation failures, `error.generic.response.json`
for `IllegalArgumentException` (400), `IllegalStateException` (409), `AccessDeniedException` (403),
`ResourceNotFoundException` (404). A missing or wrong service token is a bare 401 with no JSON body.

## Identity and time ownership

1. **The firmware owns reading identity and measurement time.** Each sample gets one UUID v4 generated
   on the device and one UTC instant captured after NTP sync. Both travel unchanged through the edge:
   `reading_id` and `measured_at` in `device-edge/telemetry.request.json`, `reading_id` and
   `occurred_at` in `core-edge/telemetry.batch.request.json`.
2. **The edge may mint a UUID only for explicitly legacy clients** that send neither field. It does so
   once, at persistence time, and stores it with the row. It cannot deduplicate a legacy device's own
   HTTP retries; that limitation is accepted and logged.
3. **Edge receipt time is metadata.** It is stored as `received_at` and never substituted for the
   measurement instant. A record without a measurement instant is a validation error at the edge
   (once the firmware sends `measured_at`), never silently timestamped with upload time.
4. **Retries are byte-identical.** The edge snapshots the outbound record when it enqueues it and
   replays that snapshot. Core returns `CREATED` for an exact replay and `VALIDATION_ERROR` for a
   changed payload under an existing `reading_id`.
5. **PM values are decimals end to end.** JSON integers remain valid numbers.

## Alert acknowledgement semantics

Core has one ACK today and it is a business action: it changes the alert to `ACKNOWLEDGED`, which is
what a user would do from the app. Therefore:

- The edge **must not** call `/api/v1/edge/alerts/{id}/ack` merely because it cached or delivered the
  alert. That was the previous edge behaviour and is removed in Phase 3.
- The device-facing incident feed (`device-edge/incidents.pending.response.json`) is a stream of
  **transitions**, one row per `(alert_id, status)`. The device acks the edge-local transition `id`.
  That ack is a delivery receipt; it does not reach core as a business ACK.
- Pending events are not a snapshot of active state. An empty page means "nothing new", not "no
  active incidents". The firmware keeps an incident active until it receives a `RESOLVED` transition
  for the same `alert_id`.

**Proposed core extension (Phase 3):** a delivery-receipt endpoint distinct from business ACK, and a
durable transition cursor (`sequence` per alert transition) for `/api/v1/edge/alerts/pending`, so the
edge can advance past old `RESOLVED` rows without missing later resolutions of older alerts. Until it
lands, the edge deduplicates by `(alert_id, status)` locally.

## Metric names

Core publishes `AlertMetric` names: `CO2`, `PM25`, `TEMPERATURE`, `HUMIDITY`. The firmware must compare
against these exact strings.

## Command types

`STANDBY`, `WAKE`, `RESTART`. Anything else is rejected by core when issued, so the edge and firmware
never see it. Firmware handlers for other names are dead code.
