# Refactor follow-up — 2026-09-07

The follow-up repairs the review findings and completes shared cleanup and architecture checks.
The original audit baseline was `90f73f0`. No configured deployment database was changed.

## Fixed

- Resetting a device whose name already equals its factory name now explicitly advances the
  roster watermark after assignment deletion. The device remains visible to incremental edge pulls.
- Roster rows are mapped from JDBC values, preserving raw hardware/API-key strings and timestamp
  instants. Interface projections had stringified value objects and produced incorrect timezone
  conversions; PostgreSQL and H2 tests now assert the wire values and cursor behavior.
- Google login explicitly saves activation/OAuth changes to existing users. The domain aggregate
  is no longer JPA-managed, so dirty checking cannot save these changes implicitly.
- Telemetry event handlers have distinct Spring bean names. Their identical simple names had
  prevented the complete application context from starting.
- Notifications consumes alerting's public integration event. That event exposes strings instead
  of alerting enums; the unused internal incident event is removed.
- Device, billing and alerting facades delegate reads to application query services. Unused
  assignment-scoped threshold facade methods are removed. The roster controller uses a query port.
- Edge publishers live under `outboundservices/edge`, depend on shared `EdgeNotifier`, and use
  bounded connection/read timeouts. Device integration events live under `interfaces/events`.
- SSE handling lives under `interfaces/rest/sse`; controllers no longer depend on infrastructure.
- Telemetry and user-registration listeners run after commit. Alert evaluation and billing plan
  initialization write in independent transactions. Push logging also uses `REQUIRES_NEW` when
  invoked by alerting's existing after-commit publisher. Storage errors are no longer mistaken
  for delivery failures and retried as a second log write.
- Flyway replaces automatic Hibernate schema updates. V1 records the old schema, V2 converts
  audit timestamps, restores both device foreign keys and removes the legacy secret column.
  The unused domain auditing class and startup DDL component are removed; OSIV is disabled.
- `ResourceNotFoundException` remains framework-free, with transport mapping owned by the REST
  handler. Its documentation no longer defines the exception in terms of an HTTP status.
- ArchUnit enforces domain purity, layer direction, facade/query separation, context contracts,
  and static persistence assemblers. Mockito instrumentation is configured explicitly for JDK 25.

## Validation

`CLAIR_TEST_POSTGRES_URL=... mvn clean verify`: **602 tests passed, zero failures, errors or skips**.
This includes full application startup, persisted Google-account linking, transaction commit and
rollback paths, PostgreSQL fresh/existing-schema migrations, orphan rejection, Hibernate schema
validation, and a PostgreSQL device-reset/roster regression test. CI runs the PostgreSQL tests too.

Regenerated PostgreSQL DDL matches phase 7 except for the 30 intended audit-column type changes.
Flyway-managed foreign keys are verified separately against PostgreSQL.

## Deployment and remaining scope

Flyway remains installed. Existing databases need the one-time baseline adoption and correct
legacy timestamp timezone described in [migrations.md](migrations.md). Fresh databases initialize
automatically. The temporary PostgreSQL cluster used for verification is disposable.

In-process after-commit events and HTTP hints are still not durable delivery: a process crash can
lose them. This change does not add an outbox or retry queue. The broader domain features in
[backlog.md](backlog.md) are separate work, not incomplete structural follow-up items.
