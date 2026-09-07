# Database migrations

Flyway is a permanent runtime dependency. Hibernate runs with `ddl-auto: validate` and creates
nothing, so **Flyway is the only thing that builds or changes the schema.** An empty database with
Flyway disabled will not start.

Two migrations exist, both in `src/main/resources/db/migration/`:

| Version | What it does |
|---|---|
| `V1__baseline.sql` | The phase-7 schema, verbatim. On an existing installation it is never executed — it exists so that installation has something to baseline onto. |
| `V2__audit_instants_and_device_integrity.sql` | Converts `created_at` / `updated_at` on 15 tables to `timestamp(6) with time zone`, restores the two device foreign keys, drops the legacy `devices.device_secret` column. |

Relevant settings, all in `src/main/resources/application.yml`:

```yaml
spring:
  flyway:
    baseline-on-migrate: ${FLYWAY_BASELINE_ON_MIGRATE:false}
    baseline-version: 1
    placeholders:
      legacyAuditZone: ${LEGACY_AUDIT_ZONE:UTC}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
```

---

# Connecting to an existing database — do this in order

This is the only path with irreversible steps. Read step 2 before starting.

## Step 0 — Take a dump

```sh
pg_dump --format=custom --file=clair-pre-flyway.dump "$DATABASE_URL"
```

V2 converts column types and drops a column. Flyway wraps each migration in a transaction and
PostgreSQL DDL is transactional, so a *failed* V2 rolls back cleanly — but a *successful* V2 run
with the wrong `LEGACY_AUDIT_ZONE` is not something Flyway can undo. The dump is the only way back
from that.

## Step 1 — Confirm the database is in the expected state

Run all four against the target database. Every one must give the answer in the right-hand column.

```sql
-- a) Never adopted before: no Flyway history table yet.
SELECT to_regclass('public.flyway_schema_history');          -- expect: NULL

-- b) Audit columns are still zone-less, i.e. V2 has not run.
SELECT count(*) FROM information_schema.columns
WHERE table_schema = 'public'
  AND column_name IN ('created_at', 'updated_at')
  AND data_type = 'timestamp without time zone';             -- expect: 30

-- c) No orphan rows. These abort V2 (see step 4).
SELECT (SELECT count(*) FROM device_assignments a
          LEFT JOIN devices d ON d.id = a.device_id WHERE d.id IS NULL) AS orphan_assignments,
       (SELECT count(*) FROM device_commands c
          LEFT JOIN devices d ON d.id = c.device_id WHERE d.id IS NULL) AS orphan_commands;
                                                             -- expect: 0, 0

-- d) Nothing you still need in the column V2 drops.
SELECT count(*) FROM devices WHERE device_secret IS NOT NULL;-- expect: 0, or accept the loss
```

If (b) returns something other than 30, the schema is not the phase-7 shape. V1 is a baseline for
*that specific state*, not a migration from an arbitrary older schema — stop and reconcile by hand.

## Step 2 — Determine `LEGACY_AUDIT_ZONE`. This is the irreversible decision.

The old mapping pinned `Instant` to a plain `timestamp` column, so the values stored are **local
wall-clock readings in whatever timezone the JVM that wrote them was running in** — not UTC, unless
that JVM happened to be on UTC. V2 reinterprets every one of them with:

```sql
created_at AT TIME ZONE '${legacyAuditZone}'
```

Name the wrong zone and all 30 columns shift by the difference, silently, with no error. The default
is `UTC`, which is only correct if your deployment ran on UTC.

To find the real answer, compare the newest audit row against real time:

```sql
SELECT max(created_at) AS newest_audit_row,
       (now() AT TIME ZONE 'UTC') AS utc_now
FROM users;
```

- The two are within minutes of each other → the writer was on UTC. Use `UTC`.
- `newest_audit_row` is about 5 hours behind `utc_now` → the writer was on Lima time. Use
  `America/Lima`.
- Any other offset → identify the zone with that offset and use its IANA name, not a fixed offset,
  so historical daylight-saving transitions are handled.

This only works if something wrote a row recently. If the newest row is old, check the `TZ`
environment variable or `/etc/localtime` of the host or container that ran the application instead.

Worked example: a legacy value of `2026-05-01 12:00:00` with `LEGACY_AUDIT_ZONE=America/Lima`
becomes `2026-05-01T17:00:00Z`.

## Step 3 — Run the first startup with baselining enabled

```sh
FLYWAY_BASELINE_ON_MIGRATE=true \
LEGACY_AUDIT_ZONE=America/Lima \
  ./mvnw spring-boot:run          # or however you start the app
```

Flyway writes one baseline row for version 1 without recreating any table, then applies V2 — exactly
one migration executed. Hibernate then validates the migrated schema; a startup failure here means
the schema did not match the mappings, and the migration has already committed, so go back to your
dump.

## Step 4 — If V2 fails on orphan rows

V2 refuses to add a foreign key over orphan rows rather than deleting your data. The whole migration
rolls back, including the timestamp conversion — the columns stay `timestamp without time zone`, so
there is no half-converted state to clean up. Find the offenders:

```sql
SELECT a.id, a.device_id FROM device_assignments a
  LEFT JOIN devices d ON d.id = a.device_id WHERE d.id IS NULL;
SELECT c.id, c.device_id FROM device_commands c
  LEFT JOIN devices d ON d.id = c.device_id WHERE d.id IS NULL;
```

Repair or delete those rows, then re-run step 3.

## Step 5 — Turn baselining back off

```sh
FLYWAY_BASELINE_ON_MIGRATE=false     # or just unset it; false is the default
```

Leave `LEGACY_AUDIT_ZONE` set or unset as you like — it is a placeholder used only by V2, which will
never run again on this database.

Baseline-on-migrate must not stay on. With it enabled, a populated database that Flyway does not
recognise gets silently adopted instead of failing, which is how a wrong database quietly becomes
the right one.

## Step 6 — Verify

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
-- expect: 1 << Flyway Baseline >> true, then 2 audit-instants... true

SELECT count(*) FROM information_schema.columns
WHERE table_schema = 'public' AND column_name IN ('created_at','updated_at')
  AND data_type = 'timestamp with time zone';                -- expect: 30

SELECT conname FROM pg_constraint
WHERE contype = 'f' AND conrelid IN ('device_assignments'::regclass, 'device_commands'::regclass);
                                                             -- expect: 2 rows
```

And spot-check that a timestamp you can date independently now reads correctly in UTC.

---

# Connecting to an empty database

Nothing to decide. Start the application; Flyway applies V1 then V2 and the schema is ready.

`FLYWAY_BASELINE_ON_MIGRATE` is irrelevant — it only applies to a populated database with no history
table. `LEGACY_AUDIT_ZONE` is also irrelevant: V2's conversion runs over zero rows, so no value of it
can be wrong. The `device_secret` drop is a no-op, since V1 never creates that column.

The only oddity is that V1 creates the audit columns zone-less and V2 immediately alters them, so V1
alone does not describe the schema you end up with. Harmless on empty tables.

---

# Afterwards

Do not edit a migration that has been applied anywhere. Flyway stores a checksum per version;
changing V1 or V2 breaks validation on every database that already ran it. Add `V3__…sql` instead.

Squashing V1 and V2 into one file is only possible while **no** database has applied them, and it
costs you the adoption path above for anyone whose database predates this branch.

## What is verified automatically

`MigrationIntegrationTest` covers fresh initialisation, a repeat startup with zero pending
migrations, existing-schema adoption with timezone conversion and pre-existing foreign keys, orphan
rejection with no partial conversion, and Hibernate validation of the migrated schema.
`PostgresRosterIntegrationTest` runs the device-reset and roster-cursor path against the migrated
schema.

Both are gated on `CLAIR_TEST_POSTGRES_URL`. **A local `mvn test` without that variable skips all
four tests and still reports green.** CI sets it (`.github/workflows/ci.yml`, `postgres:15`
service). To run them locally:

```sh
CLAIR_TEST_POSTGRES_URL=jdbc:postgresql://localhost:5432/clair_test \
CLAIR_TEST_POSTGRES_USER=clair_test \
CLAIR_TEST_POSTGRES_PASSWORD=... \
  ./mvnw clean verify
```

The two device foreign keys belong to Flyway, not to any JPA association, so they never appear in
Hibernate-generated DDL. `MigrationIntegrationTest` is the only thing that checks them.
