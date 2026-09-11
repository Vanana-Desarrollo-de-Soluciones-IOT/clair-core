package com.claircore.evaluation.infrastructure.persistence.jpa.adapters;

import com.claircore.evaluation.application.internal.commandservices.TelemetryEvaluationCommandServiceImpl;
import com.claircore.analytics.application.internal.commandservices.*;
import com.claircore.analytics.application.internal.outboundservices.acl.ExternalEvaluationService;
import com.claircore.analytics.domain.repositories.DeviceDailySummaryRepository;
import com.claircore.analytics.domain.repositories.DeviceMonthlySummaryRepository;
import com.claircore.analytics.domain.repositories.DeviceAnalyticsSnapshotRepository;
import com.claircore.analytics.domain.services.AqiCalculator;
import com.claircore.analytics.domain.model.commands.*;
import com.claircore.analytics.infrastructure.persistence.jpa.adapters.*;
import com.claircore.evaluation.interfaces.acl.TelemetryReading;
import com.claircore.evaluation.interfaces.acl.HourlyTelemetryAverage;
import java.time.LocalDate;
import java.util.List;
import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.valueobjects.*;
import com.claircore.evaluation.domain.repositories.TelemetryEvaluationRepository;
import com.claircore.evaluation.interfaces.events.TelemetryRecordedIntegrationEvent;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@EnabledIfEnvironmentVariable(named = "CLAIR_TEST_POSTGRES_URL", matches = ".+")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfiguration.class, TelemetryEvaluationRepositoryImpl.class, PostgresTelemetryIntegrationTest.Jdbc.class,
        DeviceDailySummaryRepositoryImpl.class, DeviceMonthlySummaryRepositoryImpl.class,
        DeviceAnalyticsSnapshotRepositoryImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PostgresTelemetryIntegrationTest {
    private static final String SCHEMA = "telemetry_" + UUID.randomUUID().toString().replace("-", "");
    @Autowired TelemetryEvaluationRepository repository;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired DeviceDailySummaryRepository dailies;
    @Autowired DeviceMonthlySummaryRepository monthlies;
    @Autowired DeviceAnalyticsSnapshotRepository snapshots;
    @TestConfiguration static class Jdbc {
        @Bean JdbcTemplate jdbcTemplate(DataSource dataSource) { return new JdbcTemplate(dataSource); }
    }
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

    @Test void concurrentRetriesStoreOneDecimalReadingAndPublishOneEvent() throws Exception {
        var publisher = mock(ApplicationEventPublisher.class);
        var service = new TelemetryEvaluationCommandServiceImpl(repository, publisher);
        var command = command(UUID.randomUUID(), UUID.randomUUID(), 12.45);
        var gate = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            Callable<UUID> send = () -> {
                gate.await();
                return new TransactionTemplate(transactions).execute(tx -> service.handle(command).getId());
            };
            var first = workers.submit(send);
            var second = workers.submit(send);
            gate.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo(second.get(15, TimeUnit.SECONDS));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM telemetry_evaluations WHERE device_id = ?", Long.class,
                command.deviceId().value())).isEqualTo(1);
        var stored = new TransactionTemplate(transactions).execute(tx -> repository.findLatestByDeviceId(command.deviceId().value()).orElseThrow());
        assertThat(stored.getParticulateMatter().pm2_5()).isEqualTo(12.45);
        assertThat(stored.getRecordedAt()).isEqualTo(command.recordedAt());
        assertThat(stored.getCreatedAt()).isAfter(command.recordedAt());
        verify(publisher, times(1)).publishEvent(any(TelemetryRecordedIntegrationEvent.class));
        assertThatThrownBy(() -> new TransactionTemplate(transactions).execute(tx ->
                service.handle(command(command.deviceId().value(), command.readingId(), 99.0))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("different measurement data");
        verifyNoMoreInteractions(publisher);
    }

    @Test void rerunningReportsReplacesRowsAndRepairsMonthAndNextDayComparison() {
        var source = mock(ExternalEvaluationService.class);
        var calculator = new AqiCalculator();
        var monthly = new MonthlySummaryCommandServiceImpl(dailies, monthlies);
        var daily = new DailySummaryCommandServiceImpl(source, dailies, calculator, monthly, "America/Lima");
        var tx = new TransactionTemplate(transactions);
        UUID device = UUID.randomUUID();
        LocalDate day = LocalDate.of(2026, 5, 1);
        Instant at = Instant.parse("2026-05-01T12:00:00Z");
        when(source.fetchReadings(any(), any())).thenReturn(List.of(
                new TelemetryReading(device, 400.5, 8.0, 22, 50, at)));
        tx.executeWithoutResult(status -> daily.handle(new GenerateDailySummaryCommand(day)));
        UUID originalId = tx.execute(status -> dailies.findByDeviceIdAndDate(device, day).orElseThrow().getId());
        when(source.fetchReadings(any(), any())).thenReturn(List.of(
                new TelemetryReading(device, 400.5, 9.0, 22, 50, at.plusSeconds(86400))));
        tx.executeWithoutResult(status -> daily.handle(new GenerateDailySummaryCommand(day.plusDays(1))));
        when(source.fetchReadings(any(), any())).thenReturn(List.of(
                new TelemetryReading(device, 400.5, 8.0, 22, 50, at),
                new TelemetryReading(device, 400.5, 40.0, 22, 50, at.plusSeconds(60))));
        tx.executeWithoutResult(status -> daily.handle(new GenerateDailySummaryCommand(day)));
        tx.executeWithoutResult(status -> {
            var revised = dailies.findByDeviceIdAndDate(device, day).orElseThrow();
            assertThat(revised.getId()).isEqualTo(originalId);
            assertThat(revised.getReadingCount()).isEqualTo(2);
            assertThat(revised.getAverageAqi()).isEqualTo(calculator.calculateAqi(24.0).value());
            var next = dailies.findByDeviceIdAndDate(device, day.plusDays(1)).orElseThrow();
            assertThat(next.getAqiDeltaPct()).isEqualTo((50 - revised.getAverageAqi()) * 100.0 / revised.getAverageAqi());
            var month = monthlies.findByDeviceIdAndMonth(device, day).orElseThrow();
            assertThat(month.getReadingCount()).isEqualTo(3);
            assertThat(month.getPm2_5().avg()).isEqualTo(19.0);
            assertThat(month.getAverageAqi()).isEqualTo(calculator.calculateAqi(19.0).value());
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM device_daily_summaries WHERE device_id = ?", Long.class, device)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM device_monthly_summaries WHERE device_id = ?", Long.class, device)).isEqualTo(1);
        var hourly = new SnapshotAggregationCommandServiceImpl(source, snapshots, calculator);
        when(source.fetchHourlyTelemetryAggregation(any(), any())).thenReturn(List.of(new HourlyTelemetryAverage(device, 400.5, 12.05, 22, 50)));
        tx.executeWithoutResult(status -> hourly.handle(new AggregateHourlySnapshotCommand(at)));
        tx.executeWithoutResult(status -> hourly.handle(new AggregateHourlySnapshotCommand(at)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM device_analytics_snapshots WHERE device_id = ?", Long.class, device)).isEqualTo(1);
    }

    private static EvaluateTelemetryCommand command(UUID device, UUID reading, double pm) {
        return new EvaluateTelemetryCommand(new DeviceId(device), reading, 15L,
                new AirQuality(400.5, 22.5, 45.5), new ParticulateMatter(1.1, pm, 20.3),
                new Connectivity("ONLINE", "wifi", -50), new Location("PERU"), 100, "STABLE",
                Instant.parse("2026-05-16T22:30:00.123456789Z"));
    }

    @AfterAll static void dropOwnSchema() throws Exception {
        try (var c = DriverManager.getConnection(System.getenv("CLAIR_TEST_POSTGRES_URL"),
                System.getenv().getOrDefault("CLAIR_TEST_POSTGRES_USER", "clair_test"),
                System.getenv().getOrDefault("CLAIR_TEST_POSTGRES_PASSWORD", "")); var s = c.createStatement()) {
            s.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }
}
