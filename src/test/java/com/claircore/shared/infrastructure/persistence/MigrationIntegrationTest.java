package com.claircore.shared.infrastructure.persistence;

import com.claircore.ClairCoreApplication;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import jakarta.persistence.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Runs only against an explicitly supplied disposable PostgreSQL database. */
@EnabledIfEnvironmentVariable(named = "CLAIR_TEST_POSTGRES_URL", matches = ".+")
class MigrationIntegrationTest {
    private String schema;
    private final String url = System.getenv("CLAIR_TEST_POSTGRES_URL");
    private final String user = System.getenv().getOrDefault("CLAIR_TEST_POSTGRES_USER", "clair_test");
    private final String password = System.getenv().getOrDefault("CLAIR_TEST_POSTGRES_PASSWORD", "");

    @BeforeEach void createSchema() throws Exception {
        schema = "audit_" + UUID.randomUUID().toString().replace("-", "");
        try (var c = DriverManager.getConnection(url, user, password); var s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
    }

    @AfterEach void dropOwnSchema() throws Exception {
        try (var c = DriverManager.getConnection(url, user, password); var s = c.createStatement()) {
            s.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    private org.flywaydb.core.api.configuration.FluentConfiguration configuration() {
        return Flyway.configure().dataSource(url, user, password).schemas(schema).defaultSchema(schema)
                .placeholders(Map.of("legacyAuditZone", "America/Lima"));
    }

    private Connection connection() throws Exception {
        var c = DriverManager.getConnection(url, user, password);
        c.setSchema(schema);
        return c;
    }

    @Test void freshSchemaMigratesAndMatchesHibernateAndRejectsOrphans() throws Exception {
        var flyway = configuration().load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(3);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        validateHibernate();
        try (var c = connection(); var s = c.createStatement()) {
            assertThatThrownBy(() -> s.execute("INSERT INTO device_commands (id, device_id, created_at, updated_at, status, type) VALUES (gen_random_uuid(), gen_random_uuid(), now(), now(), 'PENDING', 'STANDBY')"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("foreign key");
            assertThatThrownBy(() -> s.execute("INSERT INTO device_assignments (id, device_id, created_at, updated_at, status) VALUES (gen_random_uuid(), gen_random_uuid(), now(), now(), 'OFFLINE')"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("foreign key");
        }
    }

    @Test void adoptsExistingSchemaPreservingInstantsAndExistingForeignKeys() throws Exception {
        configuration().target("1").load().migrate();
        try (var c = connection(); var s = c.createStatement()) {
            s.execute("DROP TABLE flyway_schema_history");
            s.execute("ALTER TABLE devices ADD device_secret varchar(255)");
            s.execute("INSERT INTO devices (id, api_key, hardware_id, device_type, serial_number, name, factory_name, deleted, created_at, updated_at) VALUES ('00000000-0000-0000-0000-000000000001', 'key', 'HW-0001', 'air-quality-v1', 'SN-1', 'Sensor', 'Sensor', false, '2026-05-01 12:00:00', '2026-05-01 13:00:00')");
            s.execute("ALTER TABLE device_assignments ADD CONSTRAINT old_assignment_fk FOREIGN KEY(device_id) REFERENCES devices(id)");
            s.execute("ALTER TABLE device_commands ADD CONSTRAINT old_command_fk FOREIGN KEY(device_id) REFERENCES devices(id)");
        }
        assertThat(configuration().baselineOnMigrate(true).baselineVersion("1").load().migrate().migrationsExecuted).isEqualTo(2);
        validateHibernate();
        try (var c = connection(); var s = c.createStatement()) {
            try (var rs = s.executeQuery("SELECT created_at, updated_at FROM devices")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getTimestamp(1).toInstant()).isEqualTo(Instant.parse("2026-05-01T17:00:00Z"));
                assertThat(rs.getTimestamp(2).toInstant()).isEqualTo(Instant.parse("2026-05-01T18:00:00Z"));
            }
            try (var rs = s.executeQuery("SELECT count(*) FROM pg_constraint WHERE contype='f' AND connamespace='" + schema + "'::regnamespace AND confrelid='devices'::regclass")) {
                rs.next(); assertThat(rs.getInt(1)).isEqualTo(2);
            }
            try (var rs = s.executeQuery("SELECT count(*) FROM information_schema.columns WHERE table_schema='" + schema + "' AND column_name='device_secret'")) {
                rs.next(); assertThat(rs.getInt(1)).isZero();
            }
        }
    }

    @Test void orphanRowsAbortTheMigrationWithoutPartialTimestampConversion() throws Exception {
        configuration().target("1").load().migrate();
        try (var c = connection(); var s = c.createStatement()) {
            s.execute("INSERT INTO device_commands (id, device_id, created_at, updated_at, status, type) VALUES (gen_random_uuid(), gen_random_uuid(), now(), now(), 'PENDING', 'STANDBY')");
        }
        assertThatThrownBy(() -> configuration().load().migrate()).isInstanceOf(FlywayException.class);
        try (var c = connection(); var s = c.createStatement(); var rs = s.executeQuery("SELECT data_type FROM information_schema.columns WHERE table_schema='" + schema + "' AND table_name='devices' AND column_name='created_at'")) {
            rs.next(); assertThat(rs.getString(1)).isEqualTo("timestamp without time zone");
        }
    }

    @Test void telemetryUpgradePreservesLegacyTimesAndAssignsStableIdentity() throws Exception {
        configuration().target("2").load().migrate();
        try (var c = connection(); var statement = c.createStatement()) {
            statement.execute("""
                    INSERT INTO telemetry_evaluations
                      (id, device_id, device_time, uptime_seconds, aq_co2, aq_temperature, aq_humidity,
                       pm_pm1_0, pm_pm2_5, pm_pm10, conn_status, health_status, status, recorded_at, created_at, updated_at)
                    VALUES ('00000000-0000-0000-0000-000000000123', gen_random_uuid(), '12:30:00', 10,
                            400.5, 22, 50, 1, 12, 20, 'ONLINE', 100, 'STABLE',
                            '2026-05-01T17:30:00Z', '2026-05-01T17:31:00Z', '2026-05-01T17:31:00Z')
                    """);
        }
        assertThat(configuration().load().migrate().migrationsExecuted).isEqualTo(1);
        try (var c = connection(); var statement = c.createStatement();
             var rs = statement.executeQuery("SELECT id, reading_id, recorded_at, created_at, pm_pm2_5, pg_typeof(pm_pm2_5)::text FROM telemetry_evaluations")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getObject(2)).isEqualTo(rs.getObject(1));
            assertThat(rs.getTimestamp(3).toInstant()).isEqualTo(Instant.parse("2026-05-01T17:30:00Z"));
            assertThat(rs.getTimestamp(4).toInstant()).isEqualTo(Instant.parse("2026-05-01T17:31:00Z"));
            assertThat(rs.getDouble(5)).isEqualTo(12.0);
            assertThat(rs.getString(6)).isEqualTo("double precision");
        }
    }

    private void validateHibernate() throws Exception {
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.url", url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema)
                .applySetting("hibernate.connection.username", user).applySetting("hibernate.connection.password", password)
                .applySetting("hibernate.hbm2ddl.auto", "validate")
                .applySetting("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .applySetting("hibernate.implicit_naming_strategy", "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy")
                .build();
        try {
            var sources = new MetadataSources(registry);
            Path classes = Path.of(ClairCoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            try (var files = Files.walk(classes)) {
                for (var file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                    var name = classes.relativize(file).toString().replace(java.io.File.separatorChar, '.').replaceAll("\\.class$", "");
                    Class<?> type = Class.forName(name, false, getClass().getClassLoader());
                    if (type.isAnnotationPresent(Entity.class) || type.isAnnotationPresent(MappedSuperclass.class)
                            || type.isAnnotationPresent(Embeddable.class) || type.isAnnotationPresent(Converter.class)) {
                        sources.addAnnotatedClass(type);
                    }
                }
            }
            sources.buildMetadata().buildSessionFactory().close();
        } finally { StandardServiceRegistryBuilder.destroy(registry); }
    }
}
