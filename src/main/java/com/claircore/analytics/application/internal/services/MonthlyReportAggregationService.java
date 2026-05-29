package com.claircore.analytics.application.internal.services;

import com.claircore.analytics.domain.model.entities.DeviceDailySummary;
import com.claircore.analytics.domain.model.entities.DeviceMonthlySummary;
import com.claircore.analytics.domain.model.valueobjects.AqiCategoryBreakdown;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.MetricStats;
import com.claircore.analytics.infrastructure.persistence.jpa.repositories.DeviceDailySummaryRepository;
import com.claircore.analytics.infrastructure.persistence.jpa.repositories.DeviceMonthlySummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds {@link DeviceMonthlySummary} rows by cascading a month's
 * {@link DeviceDailySummary} rows. Averages are reading-count weighted, extremes
 * are extremes-of-extremes, and category breakdowns are summed — so the monthly
 * figures are exact, never an average-of-averages. Runs on the 1st for the month
 * that just closed; {@link #generateForMonth} is reusable for backfill.
 */
@Service
public class MonthlyReportAggregationService {

    private static final Logger logger = LoggerFactory.getLogger(MonthlyReportAggregationService.class);

    private final DeviceDailySummaryRepository dailySummaryRepository;
    private final DeviceMonthlySummaryRepository monthlySummaryRepository;
    private final ZoneId reportZone;

    public MonthlyReportAggregationService(
            DeviceDailySummaryRepository dailySummaryRepository,
            DeviceMonthlySummaryRepository monthlySummaryRepository,
            @Value("${claircore.reports.zone:America/Lima}") String reportZone
    ) {
        this.dailySummaryRepository = dailySummaryRepository;
        this.monthlySummaryRepository = monthlySummaryRepository;
        this.reportZone = ZoneId.of(reportZone);
    }

    /** Fires at 01:00 local time on the 1st and cascades the month that just closed. */
    @Scheduled(cron = "0 0 1 1 * *", zone = "${claircore.reports.zone:America/Lima}")
    public void aggregatePreviousMonth() {
        LocalDate previousMonth = LocalDate.now(reportZone).minusMonths(1).withDayOfMonth(1);
        generateForMonth(previousMonth);
    }

    /**
     * Cascades daily summaries into one monthly summary per device for the given
     * month. Idempotent: existing rows for the month are skipped.
     */
    @Transactional
    public void generateForMonth(LocalDate month) {
        LocalDate firstDay = month.withDayOfMonth(1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);

        List<DeviceDailySummary> dailies = dailySummaryRepository.findAllByDateBetween(firstDay, lastDay);

        Map<UUID, MonthlyAccumulator> byDevice = new LinkedHashMap<>();
        for (DeviceDailySummary daily : dailies) {
            byDevice.computeIfAbsent(daily.getDeviceId().value(), id -> new MonthlyAccumulator()).add(daily);
        }

        int written = 0;
        for (Map.Entry<UUID, MonthlyAccumulator> entry : byDevice.entrySet()) {
            UUID deviceId = entry.getKey();
            if (monthlySummaryRepository.existsByDeviceIdAndMonth(deviceId, firstDay)) {
                continue;
            }
            DeviceMonthlySummary summary = entry.getValue().toSummary(new DeviceId(deviceId), firstDay,
                    previousMonthAqi(deviceId, firstDay));
            monthlySummaryRepository.save(summary);
            written++;
        }
        logger.info("Monthly report aggregation for {} produced {} summaries", firstDay, written);
    }

    private Integer previousMonthAqi(UUID deviceId, LocalDate month) {
        return monthlySummaryRepository.findByDeviceIdAndMonth(deviceId, month.minusMonths(1))
                .map(DeviceMonthlySummary::getAverageAqi)
                .orElse(null);
    }

    /** Reading-count-weighted cascade over one device's daily summaries for a month. */
    private static final class MonthlyAccumulator {
        private double co2WeightedAvg, pm25WeightedAvg, tempWeightedAvg, humWeightedAvg;
        private double aqiWeightedSum;
        private double co2Min = Double.MAX_VALUE, co2Max = -Double.MAX_VALUE;
        private double pm25Min = Double.MAX_VALUE, pm25Max = -Double.MAX_VALUE;
        private double tempMin = Double.MAX_VALUE, tempMax = -Double.MAX_VALUE;
        private double humMin = Double.MAX_VALUE, humMax = -Double.MAX_VALUE;
        private double peakPm25 = -Double.MAX_VALUE;
        private Instant peakPm25At;
        private long readingCount;
        private int daysCovered;
        private AqiCategoryBreakdown breakdown = AqiCategoryBreakdown.empty();

        void add(DeviceDailySummary d) {
            long w = d.getReadingCount();
            co2WeightedAvg += d.getCo2().avg() * w;
            pm25WeightedAvg += d.getPm2_5().avg() * w;
            tempWeightedAvg += d.getTemperature().avg() * w;
            humWeightedAvg += d.getHumidity().avg() * w;
            aqiWeightedSum += (double) d.getAverageAqi() * w;
            co2Min = Math.min(co2Min, d.getCo2().min()); co2Max = Math.max(co2Max, d.getCo2().max());
            pm25Min = Math.min(pm25Min, d.getPm2_5().min()); pm25Max = Math.max(pm25Max, d.getPm2_5().max());
            tempMin = Math.min(tempMin, d.getTemperature().min()); tempMax = Math.max(tempMax, d.getTemperature().max());
            humMin = Math.min(humMin, d.getHumidity().min()); humMax = Math.max(humMax, d.getHumidity().max());
            if (d.getPeakPm2_5() > peakPm25) { peakPm25 = d.getPeakPm2_5(); peakPm25At = d.getPeakPm2_5At(); }
            breakdown = breakdown.plus(d.getCategoryBreakdown());
            readingCount += w;
            daysCovered++;
        }

        DeviceMonthlySummary toSummary(DeviceId deviceId, LocalDate month, Integer previousMonthAqi) {
            int averageAqi = (int) Math.round(aqiWeightedSum / readingCount);
            Double deltaPct = (previousMonthAqi != null && previousMonthAqi > 0)
                    ? ((averageAqi - previousMonthAqi) * 100.0) / previousMonthAqi
                    : null;
            return new DeviceMonthlySummary(
                    deviceId,
                    month,
                    MetricStats.of(co2WeightedAvg / readingCount, co2Min, co2Max),
                    MetricStats.of(pm25WeightedAvg / readingCount, pm25Min, pm25Max),
                    MetricStats.of(tempWeightedAvg / readingCount, tempMin, tempMax),
                    MetricStats.of(humWeightedAvg / readingCount, humMin, humMax),
                    peakPm25,
                    peakPm25At,
                    averageAqi,
                    breakdown.dominant(),
                    breakdown,
                    readingCount,
                    daysCovered,
                    deltaPct
            );
        }
    }
}
