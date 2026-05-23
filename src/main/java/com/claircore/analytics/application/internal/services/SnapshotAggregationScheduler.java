package com.claircore.analytics.application.internal.services;

import com.claircore.analytics.domain.model.entities.DeviceAnalyticsSnapshot;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.services.AqiCalculationDomainService;
import com.claircore.analytics.infrastructure.persistence.jpa.repositories.DeviceAnalyticsSnapshotRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SnapshotAggregationScheduler {

    private final JdbcTemplate jdbcTemplate;
    private final DeviceAnalyticsSnapshotRepository snapshotRepository;
    private final AqiCalculationDomainService aqiCalculationDomainService;

    public SnapshotAggregationScheduler(
            JdbcTemplate jdbcTemplate,
            DeviceAnalyticsSnapshotRepository snapshotRepository,
            AqiCalculationDomainService aqiCalculationDomainService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.snapshotRepository = snapshotRepository;
        this.aqiCalculationDomainService = aqiCalculationDomainService;
    }

    //@Scheduled(fixedRate = 5000)//
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void aggregateHourlySnapshots() {

        System.out.println("Run 5 segundos");
        // Cambiamos la ventana de tiempo para mirar los últimos 5 minutos en lugar de la hora pasada
        Instant windowEnd = Instant.now();
        Instant windowStart = windowEnd.minus(Duration.ofMinutes(5));

        String sql = """
                SELECT device_id,
                       AVG(aq_co2) as avg_co2,
                       AVG(pm_pm2_5) as avg_pm2_5,
                       AVG(aq_temperature) as avg_temperature,
                       AVG(aq_humidity) as avg_humidity
                FROM telemetry_evaluations
                WHERE recorded_at >= ? AND recorded_at < ?
                GROUP BY device_id
                """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                sql,
                Timestamp.from(windowStart),
                Timestamp.from(windowEnd)
        );

        for (Map<String, Object> row : rows) {
            // SOLUCION AL CLASS CAST EXCEPTION
            Object deviceIdObj = row.get("device_id");
            UUID deviceId = deviceIdObj instanceof UUID ? 
                    (UUID) deviceIdObj : 
                    UUID.fromString(deviceIdObj.toString());
            Double avgCo2 = ((Number) row.get("avg_co2")).doubleValue();
            Double avgPm25 = ((Number) row.get("avg_pm2_5")).doubleValue();
            Double avgTemp = ((Number) row.get("avg_temperature")).doubleValue();
            Double avgHum = ((Number) row.get("avg_humidity")).doubleValue();

            var aqi = aqiCalculationDomainService.calculateAqi(avgPm25, avgCo2);

            var snapshot = new DeviceAnalyticsSnapshot(
                    new DeviceId(deviceId),
                    windowStart,
                    windowEnd,
                    avgCo2,
                    avgPm25,
                    avgTemp,
                    avgHum,
                    aqi
            );

            snapshotRepository.save(snapshot);
        }
    }
}
