# Database migration adoption

Flyway is a permanent runtime dependency. It replaces Hibernate schema updates; Hibernate now
runs with `ddl-auto: validate` and Open Session in View is disabled. No configured deployment
database was modified while implementing this change.

## Empty database

Start the application normally. Flyway applies V1 (the recorded phase-7 schema), then V2.
V2 converts the 15 tables' audit columns to `timestamp(6) with time zone`, restores the
assignment/command foreign keys to devices, and removes the legacy `device_secret` column.
The application does not need JPA associations to retain database foreign keys.

## Existing database

1. Confirm the existing schema matches the phase-7 tables and columns. V1 is a baseline for
   that state; it is not a migration from an arbitrary older schema.
2. Set `LEGACY_AUDIT_ZONE` to the timezone used to write the old audit timestamps. The default
   is UTC, but a deployment writing local Lima wall-clock values must use `America/Lima`.
   For example, legacy `2026-05-01 12:00:00` in Lima becomes `2026-05-01T17:00:00Z`.
3. For the first startup only, set `FLYWAY_BASELINE_ON_MIGRATE=true`. Flyway records version 1
   without recreating tables, then applies V2. Set it back to false after adoption.
4. Subsequent startups validate checksums and apply only new migrations. Do not edit migrations
   after deployment; add a new version instead.

Baseline-on-migrate is false by default, so an unrecognized populated database fails instead of
being silently adopted. Existing foreign keys are detected regardless of their generated names;
V2 does not duplicate them. Orphan assignments/commands cause V2 to fail and roll back rather
than deleting data. Resolve the orphan references before retrying.

## What was verified

The PostgreSQL tests cover fresh initialization, repeat startup with zero pending migrations,
existing-schema adoption with timezone conversion and existing foreign keys, orphan rejection,
and Hibernate validation of the migrated schema. The roster integration test runs the actual
reset command against the migrated PostgreSQL schema.

The offline DDL snapshot covers ORM mappings only. The two device foreign keys belong to
Flyway and are intentionally absent from generated Hibernate DDL; migration tests verify them.
