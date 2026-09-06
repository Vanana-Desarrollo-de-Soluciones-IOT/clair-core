# DDL control gate

Every phase of `refactor.md` must leave the generated schema unchanged except for
explicitly listed removals. `DdlGate.java` regenerates that schema offline: it scans
`target/classes` for every `@Entity`, `@MappedSuperclass`, `@Embeddable` and `@Converter`
and writes the PostgreSQL DDL Hibernate would emit. It opens no database connection, so it
is safe to run against any working tree.

```sh
mvn -q -DskipTests compile
mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
CP="$(cat /tmp/cp.txt):target/classes"
javac -cp "$CP" -d /tmp docs/audit/ddl/DdlGate.java
java -cp "$CP:/tmp" DdlGate target/classes /tmp/schema.sql
diff <(sort docs/audit/ddl/schema-phase-<n-1>.sql) <(sort /tmp/schema.sql)
```

The diff must be empty, or contain only the removals the phase declares. Commit the new
snapshot as `schema-phase-<n>.sql` at the end of each phase.

Snapshots use the Postgres dialect; H2 output differs and is not a valid control.

| Snapshot | Declared diff against the previous one |
|---|---|
| `schema-phase-0.sql` | `outbox_message` table and `idx_outbox_unpublished` removed (0 consumers) |
| `schema-phase-1.sql` | none; byte-identical to phase 0 |
| `schema-phase-2.sql` | none; byte-identical to phase 1 |
| `schema-phase-3.sql` | none; byte-identical to phase 2 |
