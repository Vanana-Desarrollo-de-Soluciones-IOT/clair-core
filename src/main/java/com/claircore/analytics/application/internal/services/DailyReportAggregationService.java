package com.claircore.analytics.application.internal.services;

import com.claircore.analytics.domain.model.entities.DeviceDailySummary;
import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;
import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import com.claircore.analytics.domain.model.valueobjects.AqiCategoryBreakdown;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.MetricStats;
import com.claircore.analytics.domain.services.AqiCalculationDomainService;
import com.claircore.analytics.infrastructure.persistence.jpa.repositories.DeviceDailySummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds {@link DeviceDailySummary} rows from raw telemetry, one per device per
 * calendar day in the configured zone. Runs nightly for "yesterday"; the same
 * {@link #generateForDate} entry point can be reused to backfill historical days
 * (raw telemetry is never purged).
 */
@Service
public class DailyReportAggregationService {

    private static final Logger logger = LoggerFactory.getLogger(DailyReportAggregationService.class);

    private final JdbcTemplate jdbcTemplate;
    private final DeviceDailySummaryRepository dailySummaryRepository;
    private final AqiCalculationDomainService aqiCalculationDomainService;
    private final ZoneId reportZone;

    public DailyReportAggregationService(
            JdbcTemplate jdbcTemplate,
            DeviceDailySummaryRepository dailySummaryRepository,
            AqiCalculationDomainService aqiCalculationDomainService,
            @Value("${claircore.reports.zone:America/Lima}") String reportZone
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.dailySummaryRepository = dailySummaryRepository;
        this.aqiCalculationDomainService = aqiCalculationDomainService;
        this.reportZone = ZoneId.of(reportZone);
    }

    /** Fires at 00:15 local time and summarises the day that just closed. */
    @Scheduled(cron = "0 15 0 * * *", zone = "${claircore.reports.zone:America/Lima}")
    public void aggregatePreviousDay() {
        LocalDate yesterday = LocalDate.now(reportZone).minusDays(1);
        generateForDate(yesterday);
    }

    /**
     * Computes and persists a daily summary per device for the given local date.
     * Idempotent: devices that already have a row for the date are skipped.
     */
    @Transactional
    public void generateForDate(LocalDate date) {
        Instant windowStart = date.atStartOfDay(reportZone).toInstant();
        Instant windowEnd = date.plusDays(1).atStartOfDay(reportZone).toInstant();

        String sql = """
                SELECT device_id, aq_co2, pm_pm2_5, aq_temperature, aq_humidity, recorded_at
                FROM telemetry_evaluations
                WHERE recorded_at >= ? AND recorded_at < ?
                ORDER BY device_id, recorded_at
                """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                sql, Timestamp.from(windowStart), Timestamp.from(windowEnd));

        Map<UUID, DailyAccumulator> byDevice = new HashMap<>();
        for (Map<String, Object> row : rows) {
            UUID deviceId = toUuid(row.get("device_id"));
            byDevice.computeIfAbsent(deviceId, id -> new DailyAccumulator())
                    .add(
                            ((Number) row.get("aq_co2")).doubleValue(),
                            ((Number) row.get("pm_pm2_5")).doubleValue(),
                            ((Number) row.get("aq_temperature")).doubleValue(),
                            ((Number) row.get("aq_humidity")).doubleValue(),
                            ((Timestamp) row.get("recorded_at")).toInstant()
                    );
        }

        int written = 0;
        for (Map.Entry<UUID, DailyAccumulator> entry : byDevice.entrySet()) {
            UUID deviceId = entry.getKey();
            if (dailySummaryRepository.existsByDeviceIdAndDate(deviceId, date)) {
                continue;
            }
            DeviceDailySummary summary = entry.getValue().toSummary(new DeviceId(deviceId), date,
                    previousDayAqi(deviceId, date));
            dailySummaryRepository.save(summary);
            written++;
        }
        logger.info("Daily report aggregation for {} produced {} summaries", date, written);
    }

    private Integer previousDayAqi(UUID deviceId, LocalDate date) {
        return dailySummaryRepository.findByDeviceIdAndDate(deviceId, date.minusDays(1))
                .map(DeviceDailySummary::getAverageAqi)
                .orElse(null);
    }

    private UUID toUuid(Object value) {
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }

    /** Single-pass accumulator over one device's readings for a day. */
    private final class DailyAccumulator {
        private double co2Sum, tempSum, humSum, pm25Sum;
        private double co2Min = Double.MAX_VALUE, co2Max = -Double.MAX_VALUE;
        private double tempMin = Double.MAX_VALUE, tempMax = -Double.MAX_VALUE;
        private double humMin = Double.MAX_VALUE, humMax = -Double.MAX_VALUE;
        private double pm25Min = Double.MAX_VALUE, pm25Max = -Double.MAX_VALUE;
        private double peakPm25 = -Double.MAX_VALUE;
        private Instant peakPm25At;
        private long aqiSum;
        private long count;
        private final Map<AqiCategory, Long> categoryCounts = new EnumMap<>(AqiCategory.class);

        void add(double co2, double pm25, double temp, double hum, Instant at) {
            co2Sum += co2; tempSum += temp; humSum += hum; pm25Sum += pm25;
            co2Min = Math.min(co2Min, co2); co2Max = Math.max(co2Max, co2);
            tempMin = Math.min(tempMin, temp); tempMax = Math.max(tempMax, temp);
            humMin = Math.min(humMin, hum); humMax = Math.max(humMax, hum);
            pm25Min = Math.min(pm25Min, pm25); pm25Max = Math.max(pm25Max, pm25);
            if (pm25 > peakPm25) { peakPm25 = pm25; peakPm25At = at; }
            AirQualityIndex aqi = aqiCalculationDomainService.calculateAqi(pm25, co2);
            aqiSum += aqi.value();
            categoryCounts.merge(aqi.category(), 1L, Long::sum);
            count++;
        }

        DeviceDailySummary toSummary(DeviceId deviceId, LocalDate date, Integer previousDayAqi) {
            int averageAqi = (int) Math.round((double) aqiSum / count);
            AqiCategoryBreakdown breakdown = new AqiCategoryBreakdown(
                    categoryCounts.getOrDefault(AqiCategory.GOOD, 0L),
                    categoryCounts.getOrDefault(AqiCategory.MODERATE, 0L),
                    categoryCounts.getOrDefault(AqiCategory.UNHEALTHY_FOR_SENSITIVE, 0L),
                    categoryCounts.getOrDefault(AqiCategory.UNHEALTHY, 0L),
                    categoryCounts.getOrDefault(AqiCategory.VERY_UNHEALTHY, 0L),
                    categoryCounts.getOrDefault(AqiCategory.HAZARDOUS, 0L)
            );
            Double deltaPct = (previousDayAqi != null && previousDayAqi > 0)
                    ? ((averageAqi - previousDayAqi) * 100.0) / previousDayAqi
                    : null;
            return new DeviceDailySummary(
                    deviceId,
                    date,
                    MetricStats.of(co2Sum / count, co2Min, co2Max),
                    MetricStats.of(pm25Sum / count, pm25Min, pm25Max),
                    MetricStats.of(tempSum / count, tempMin, tempMax),
                    MetricStats.of(humSum / count, humMin, humMax),
                    peakPm25,
                    peakPm25At,
                    averageAqi,
                    breakdown.dominant(),
                    breakdown,
                    count,
                    deltaPct
            );
        }
    }
}
