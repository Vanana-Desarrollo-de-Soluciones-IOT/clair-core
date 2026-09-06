package com.claircore.analytics.application.internal.commandservices;

import com.claircore.analytics.application.commandservices.DailySummaryCommandService;
import com.claircore.analytics.application.internal.outboundservices.acl.ExternalEvaluationService;
import com.claircore.analytics.domain.model.aggregates.DeviceDailySummary;
import com.claircore.analytics.domain.model.commands.GenerateDailySummaryCommand;
import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;
import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import com.claircore.analytics.domain.model.valueobjects.AqiCategoryBreakdown;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.MetricStats;
import com.claircore.analytics.domain.repositories.DeviceDailySummaryRepository;
import com.claircore.analytics.domain.services.AqiCalculator;
import com.claircore.evaluation.interfaces.acl.TelemetryReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builds {@link DeviceDailySummary} rows from raw telemetry, one per device per calendar day in the
 * configured zone. The readings come from the evaluation context's facade rather than a query
 * against its table, so analytics no longer depends on that table's column names.
 *
 * <p>Historical days can be backfilled by issuing the command for them; raw telemetry is never
 * purged.
 */
@Service
public class DailySummaryCommandServiceImpl implements DailySummaryCommandService {

    private static final Logger logger = LoggerFactory.getLogger(DailySummaryCommandServiceImpl.class);

    private final ExternalEvaluationService externalEvaluationService;
    private final DeviceDailySummaryRepository dailySummaryRepository;
    private final AqiCalculator aqiCalculator;
    private final ZoneId reportZone;

    public DailySummaryCommandServiceImpl(
            ExternalEvaluationService externalEvaluationService,
            DeviceDailySummaryRepository dailySummaryRepository,
            AqiCalculator aqiCalculator,
            @Value("${claircore.reports.zone:America/Lima}") String reportZone
    ) {
        this.externalEvaluationService = externalEvaluationService;
        this.dailySummaryRepository = dailySummaryRepository;
        this.aqiCalculator = aqiCalculator;
        this.reportZone = ZoneId.of(reportZone);
    }

    /** Idempotent: devices that already have a row for the date are skipped. */
    @Override
    @Transactional
    public int handle(GenerateDailySummaryCommand command) {
        LocalDate date = command.date();
        Instant windowStart = date.atStartOfDay(reportZone).toInstant();
        Instant windowEnd = date.plusDays(1).atStartOfDay(reportZone).toInstant();

        Map<UUID, DailyAccumulator> byDevice = new HashMap<>();
        for (TelemetryReading reading : externalEvaluationService.fetchReadings(windowStart, windowEnd)) {
            byDevice.computeIfAbsent(reading.deviceId(), id -> new DailyAccumulator())
                    .add(reading.co2(), reading.pm2_5(), reading.temperature(), reading.humidity(),
                            reading.recordedAt());
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
        return written;
    }

    private Integer previousDayAqi(UUID deviceId, LocalDate date) {
        return dailySummaryRepository.findByDeviceIdAndDate(deviceId, date.minusDays(1))
                .map(DeviceDailySummary::getAverageAqi)
                .orElse(null);
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
            AirQualityIndex aqi = aqiCalculator.calculateAqi(pm25, co2);
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
