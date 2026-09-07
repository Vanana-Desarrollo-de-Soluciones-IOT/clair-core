package com.claircore.device.infrastructure.persistence.jpa.adapters;

import com.claircore.device.application.internal.commandservices.DeviceCommandServiceImpl;
import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.application.internal.outboundservices.edge.ProvisioningDevicesChangedPublisher;
import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.commands.ResetDeviceAssignmentCommand;
import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.device.domain.repositories.*;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.sql.DriverManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@EnabledIfEnvironmentVariable(named = "CLAIR_TEST_POSTGRES_URL", matches = ".+")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfiguration.class, DeviceRepositoryImpl.class, DeviceAssignmentRepositoryImpl.class,
        DeviceCommandServiceImpl.class})
class PostgresRosterIntegrationTest {
    private static final String SCHEMA = "roster_" + UUID.randomUUID().toString().replace("-", "");
    @Autowired DeviceRepository devices;
    @Autowired DeviceAssignmentRepository assignments;
    @Autowired DeviceCommandServiceImpl commands;
    @MockitoBean SpaceRepository spaces;
    @MockitoBean OrganizationRepository organizations;
    @MockitoBean ExternalBillingService billing;
    @MockitoBean ProvisioningDevicesChangedPublisher hints;

    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("CLAIR_TEST_POSTGRES_URL"));
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("CLAIR_TEST_POSTGRES_USER", "clair_test"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("CLAIR_TEST_POSTGRES_PASSWORD", ""));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.hikari.schema", () -> SCHEMA);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
    }

    @Test void resetPreservesTheIncrementalRosterAndItsWireValues() {
        var device = devices.save(new Device("SN-PG", "Sensor", new HardwareId("HW-0099"),
                ApiKey.generate(), new DeviceType("air-quality-v1")));
        var owner = new UserId(UUID.randomUUID());
        var assignment = new DeviceAssignment(device.getId(), ClaimToken.generate());
        assignment.claimToSpace(UUID.randomUUID(), owner);
        assignment.markOnline();
        assignments.save(assignment);
        var before = devices.findProvisionedDevices(null, null, 10).items().getFirst();
        assertThat(before.hardwareId()).isEqualTo("HW-0099");
        assertThat(before.apiKey()).isEqualTo(device.getApiKey().value());
        assertThat(before.status()).isEqualTo(DeviceStatus.ONLINE);
        assertThat(devices.findProvisionedDevices(before.updatedAt(), device.getId(), 10).items()).isEmpty();

        commands.handle(new ResetDeviceAssignmentCommand(device.getId(), owner));

        assertThat(devices.findProvisionedDevices(before.updatedAt(), device.getId(), 10).items())
                .singleElement().satisfies(row -> {
                    assertThat(row.updatedAt()).isAfter(before.updatedAt());
                    assertThat(row.status()).isEqualTo(DeviceStatus.OFFLINE);
                    assertThat(row.deleted()).isFalse();
                });
    }

    @AfterAll static void dropOwnSchema() throws Exception {
        try (var c = DriverManager.getConnection(System.getenv("CLAIR_TEST_POSTGRES_URL"),
                System.getenv().getOrDefault("CLAIR_TEST_POSTGRES_USER", "clair_test"),
                System.getenv().getOrDefault("CLAIR_TEST_POSTGRES_PASSWORD", "")); var s = c.createStatement()) {
            s.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }
}
