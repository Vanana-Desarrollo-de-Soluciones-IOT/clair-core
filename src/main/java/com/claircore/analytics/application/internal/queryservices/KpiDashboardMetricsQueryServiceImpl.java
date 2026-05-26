package com.claircore.analytics.application.internal.queryservices;

import com.claircore.analytics.application.internal.services.KpiLiveMetricsCache;
import com.claircore.analytics.domain.model.queries.GetDashboardMetricsQuery;
import com.claircore.analytics.domain.model.valueobjects.KpiDashboardMetrics;
import com.claircore.analytics.domain.services.AqiCalculationDomainService;
import com.claircore.analytics.domain.services.KpiDashboardMetricsQueryService;
import com.claircore.analytics.domain.services.TrendAnalysisDomainService;
import com.claircore.analytics.infrastructure.persistence.jpa.repositories.DeviceAnalyticsSnapshotRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class KpiDashboardMetricsQueryServiceImpl implements KpiDashboardMetricsQueryService {

    private final KpiLiveMetricsCache liveMetricsCache;
    private final AqiCalculationDomainService aqiCalculationDomainService;
    private final TrendAnalysisDomainService trendAnalysisDomainService;
    private final DeviceAnalyticsSnapshotRepository snapshotRepository;
    private final JdbcTemplate jdbcTemplate;

    public KpiDashboardMetricsQueryServiceImpl(
            KpiLiveMetricsCache liveMetricsCache,
            AqiCalculationDomainService aqiCalculationDomainService,
            TrendAnalysisDomainService trendAnalysisDomainService,
            DeviceAnalyticsSnapshotRepository snapshotRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.liveMetricsCache = liveMetricsCache;
        this.aqiCalculationDomainService = aqiCalculationDomainService;
        this.trendAnalysisDomainService = trendAnalysisDomainService;
        this.snapshotRepository = snapshotRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KpiDashboardMetrics> handle(GetDashboardMetricsQuery query) {
        String period = query.period();
        Instant startDate = query.startDate();
        Instant endDate = query.endDate();

        boolean isLive = false;
        if (startDate == null || endDate == null) {
            if (period == null || period.equalsIgnoreCase("LIVE")) {
                isLive = true;
            }
        }

        if (isLive) {
            var buffer = liveMetricsCache.getIfPresent(query.deviceId().value());
            if (buffer == null || buffer.isEmpty()) {
                return Optional.empty();
            }

            var avg = buffer.computeAverages();
            var aqi = aqiCalculationDomainService.calculateAqi(avg.pm2_5(), avg.co2());

            var latestSnapshot = snapshotRepository
                    .findLatestByDeviceId(query.deviceId().value(), PageRequest.of(0, 1))
                    .stream().findFirst();

            double prevCo2 = latestSnapshot.map(s -> s.getAverageCo2()).orElse(avg.co2());
            double prevPm25 = latestSnapshot.map(s -> s.getAveragePm2_5()).orElse(avg.pm2_5());
            double prevTemp = latestSnapshot.map(s -> s.getAverageTemperature()).orElse(avg.temperature());
            double prevHum = latestSnapshot.map(s -> s.getAverageHumidity()).orElse(avg.humidity());

            var metrics = new KpiDashboardMetrics(
                    aqi,
                    avg.co2(),
                    avg.pm2_5(),
                    avg.temperature(),
                    avg.humidity(),
                    trendAnalysisDomainService.calculateTrend(avg.co2(), prevCo2),
                    trendAnalysisDomainService.calculateTrend(avg.pm2_5(), prevPm25),
                    trendAnalysisDomainService.calculateTrend(avg.temperature(), prevTemp),
                    trendAnalysisDomainService.calculateTrend(avg.humidity(), prevHum),
                    Instant.now()
            );

            return Optional.of(metrics);
        } else {
            Instant start;
            Instant end;

            if (startDate != null && endDate != null) {
                start = startDate;
                end = endDate;
            } else {
                Instant now = Instant.now();
                if (period != null) {
                    switch (period.toUpperCase()) {
                        case "DAY" -> {
                            start = now.minus(Duration.ofDays(1));
                            end = now;
                        }
                        case "WEEK" -> {
                            start = now.minus(Duration.ofDays(7));
                            end = now;
                        }
                        case "MONTH" -> {
                            start = now.minus(Duration.ofDays(30));
                            end = now;
                        }
                        default -> {
                            start = now.minus(Duration.ofDays(1));
                            end = now;
                        }
                    }
                } else {
                    start = now.minus(Duration.ofDays(1));
                    end = now;
                }
            }

            var avg = getAverages(query.deviceId().value(), start, end);
            var aqi = aqiCalculationDomainService.calculateAqi(avg.pm2_5(), avg.co2());

            // Para las tendencias comparamos con el período anterior de igual duración
            Duration duration = Duration.between(start, end);
            Instant prevStart = start.minus(duration);
            Instant prevEnd = start;

            var prevAvg = getAverages(query.deviceId().value(), prevStart, prevEnd);

            var metrics = new KpiDashboardMetrics(
                    aqi,
                    avg.co2(),
                    avg.pm2_5(),
                    avg.temperature(),
                    avg.humidity(),
                    trendAnalysisDomainService.calculateTrend(avg.co2(), prevAvg.co2()),
                    trendAnalysisDomainService.calculateTrend(avg.pm2_5(), prevAvg.pm2_5()),
                    trendAnalysisDomainService.calculateTrend(avg.temperature(), prevAvg.temperature()),
                    trendAnalysisDomainService.calculateTrend(avg.humidity(), prevAvg.humidity()),
                    Instant.now()
            );

            return Optional.of(metrics);
        }
    }

    private Averages getAverages(UUID deviceId, Instant start, Instant end) {
        long hours = Duration.between(start, end).toHours();
        if (hours <= 24) {
            return getRawAverages(deviceId, start, end);
        } else {
            return getSnapshotAverages(deviceId, start, end);
        }
    }

    private Averages getRawAverages(UUID deviceId, Instant start, Instant end) {
        String sql = """
                SELECT COALESCE(AVG(aq_co2), 0.0) as avg_co2,
                       COALESCE(AVG(pm_pm2_5), 0.0) as avg_pm2_5,
                       COALESCE(AVG(aq_temperature), 0.0) as avg_temperature,
                       COALESCE(AVG(aq_humidity), 0.0) as avg_humidity
                FROM telemetry_evaluations
                WHERE device_id = ? AND recorded_at >= ? AND recorded_at < ?
                """;
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    sql,
                    deviceId,
                    java.sql.Timestamp.from(start),
                    java.sql.Timestamp.from(end)
            );
            double co2 = ((Number) row.getOrDefault("avg_co2", 0.0)).doubleValue();
            double pm25 = ((Number) row.getOrDefault("avg_pm2_5", 0.0)).doubleValue();
            double temp = ((Number) row.getOrDefault("avg_temperature", 0.0)).doubleValue();
            double hum = ((Number) row.getOrDefault("avg_humidity", 0.0)).doubleValue();
            return new Averages(co2, pm25, temp, hum);
        } catch (Exception e) {
            return new Averages(0.0, 0.0, 0.0, 0.0);
        }
    }

    private Averages getSnapshotAverages(UUID deviceId, Instant start, Instant end) {
        var snapshots = snapshotRepository.findByDeviceIdAndTimeWindowStartBetween(deviceId, start, end);
        if (snapshots.isEmpty()) {
            return new Averages(0.0, 0.0, 0.0, 0.0);
        }
        double sumCo2 = 0.0;
        double sumPm25 = 0.0;
        double sumTemp = 0.0;
        double sumHum = 0.0;
        for (var s : snapshots) {
            sumCo2 += s.getAverageCo2();
            sumPm25 += s.getAveragePm2_5();
            sumTemp += s.getAverageTemperature();
            sumHum += s.getAverageHumidity();
        }
        int count = snapshots.size();
        return new Averages(sumCo2 / count, sumPm25 / count, sumTemp / count, sumHum / count);
    }

    private record Averages(double co2, double pm2_5, double temperature, double humidity) {}
}
