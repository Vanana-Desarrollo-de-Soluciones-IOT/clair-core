package com.claircore.analytics.application.internal.queryservices;

import com.claircore.analytics.application.internal.outboundservices.cache.LiveMetricsStore;
import com.claircore.analytics.application.queryservices.KpiDashboardMetricsQueryService;
import com.claircore.analytics.domain.exceptions.DeviceTelemetryUnavailableException;
import com.claircore.analytics.domain.model.queries.GetDashboardMetricsQuery;
import com.claircore.analytics.domain.model.valueobjects.KpiDashboardMetrics;
import com.claircore.analytics.domain.model.valueobjects.MetricAverages;
import com.claircore.analytics.domain.model.valueobjects.TrendPeriod;
import com.claircore.analytics.domain.repositories.DeviceAnalyticsSnapshotRepository;
import com.claircore.analytics.domain.services.AqiCalculationDomainService;
import com.claircore.analytics.domain.services.TrendAnalysisDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class KpiDashboardMetricsQueryServiceImpl implements KpiDashboardMetricsQueryService {

    private static final Duration DEFAULT_WINDOW = Duration.ofDays(1);

    private final LiveMetricsStore liveMetricsStore;
    private final AqiCalculationDomainService aqiCalculationDomainService;
    private final TrendAnalysisDomainService trendAnalysisDomainService;
    private final DeviceAnalyticsSnapshotRepository snapshotRepository;

    public KpiDashboardMetricsQueryServiceImpl(
            LiveMetricsStore liveMetricsStore,
            AqiCalculationDomainService aqiCalculationDomainService,
            TrendAnalysisDomainService trendAnalysisDomainService,
            DeviceAnalyticsSnapshotRepository snapshotRepository
    ) {
        this.liveMetricsStore = liveMetricsStore;
        this.aqiCalculationDomainService = aqiCalculationDomainService;
        this.trendAnalysisDomainService = trendAnalysisDomainService;
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KpiDashboardMetrics> handle(GetDashboardMetricsQuery query) {
        boolean hasExplicitWindow = query.startDate() != null && query.endDate() != null;
        if (!hasExplicitWindow && (query.period() == null || query.period() == TrendPeriod.LIVE)) {
            return liveMetrics(query.deviceId().value());
        }
        return Optional.of(historicalMetrics(query, hasExplicitWindow));
    }

    private Optional<KpiDashboardMetrics> liveMetrics(UUID deviceId) {
        var buffer = liveMetricsStore.getIfPresent(deviceId);
        if (buffer == null || buffer.isEmpty()) {
            return Optional.empty();
        }

        var avg = buffer.computeAverages();
        var aqi = aqiCalculationDomainService.calculateAqi(avg.pm2_5(), avg.co2());

        // The last stored snapshot is the "previous" the live window is compared against; with none,
        // the trend compares the window to itself and reads as flat rather than as a spike.
        var latest = snapshotRepository.findLatestByDeviceId(deviceId);

        return Optional.of(new KpiDashboardMetrics(
                aqi,
                avg.co2(),
                avg.pm2_5(),
                avg.temperature(),
                avg.humidity(),
                trendAnalysisDomainService.calculateTrend(avg.co2(),
                        latest.map(s -> s.getAverageCo2()).orElse(avg.co2())),
                trendAnalysisDomainService.calculateTrend(avg.pm2_5(),
                        latest.map(s -> s.getAveragePm2_5()).orElse(avg.pm2_5())),
                trendAnalysisDomainService.calculateTrend(avg.temperature(),
                        latest.map(s -> s.getAverageTemperature()).orElse(avg.temperature())),
                trendAnalysisDomainService.calculateTrend(avg.humidity(),
                        latest.map(s -> s.getAverageHumidity()).orElse(avg.humidity())),
                Instant.now()
        ));
    }

    private KpiDashboardMetrics historicalMetrics(GetDashboardMetricsQuery query, boolean hasExplicitWindow) {
        UUID deviceId = query.deviceId().value();
        Instant end = hasExplicitWindow ? query.endDate() : Instant.now();
        Instant start = hasExplicitWindow ? query.startDate() : end.minus(windowOf(query.period()));

        var averages = snapshotRepository.findAveragesByDeviceIdAndWindow(deviceId, start, end)
                .orElseThrow(() -> new DeviceTelemetryUnavailableException(deviceId, false));
        var aqi = aqiCalculationDomainService.calculateAqi(averages.pm2_5(), averages.co2());

        // Trends compare the window against the window of equal length that precedes it.
        Duration duration = Duration.between(start, end);
        var previous = snapshotRepository.findAveragesByDeviceIdAndWindow(deviceId, start.minus(duration), start);

        return new KpiDashboardMetrics(
                aqi,
                averages.co2(),
                averages.pm2_5(),
                averages.temperature(),
                averages.humidity(),
                trendAnalysisDomainService.calculateTrend(averages.co2(), previous.map(MetricAverages::co2).orElse(null)),
                trendAnalysisDomainService.calculateTrend(averages.pm2_5(), previous.map(MetricAverages::pm2_5).orElse(null)),
                trendAnalysisDomainService.calculateTrend(averages.temperature(), previous.map(MetricAverages::temperature).orElse(null)),
                trendAnalysisDomainService.calculateTrend(averages.humidity(), previous.map(MetricAverages::humidity).orElse(null)),
                Instant.now()
        );
    }

    private static Duration windowOf(TrendPeriod period) {
        if (period == null) return DEFAULT_WINDOW;
        return switch (period) {
            case LIVE, DAY -> DEFAULT_WINDOW;
            case WEEK -> Duration.ofDays(7);
            case MONTH -> Duration.ofDays(30);
        };
    }
}
