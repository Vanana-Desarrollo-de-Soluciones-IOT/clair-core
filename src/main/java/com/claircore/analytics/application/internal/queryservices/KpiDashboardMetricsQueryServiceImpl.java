package com.claircore.analytics.application.internal.queryservices;

import com.claircore.analytics.application.internal.services.KpiLiveMetricsCache;
import com.claircore.analytics.domain.model.queries.GetLiveDashboardMetricsQuery;
import com.claircore.analytics.domain.model.valueobjects.KpiDashboardMetrics;
import com.claircore.analytics.domain.services.AqiCalculationDomainService;
import com.claircore.analytics.domain.services.KpiDashboardMetricsQueryService;
import com.claircore.analytics.domain.services.TrendAnalysisDomainService;
import com.claircore.analytics.infrastructure.persistence.jpa.repositories.DeviceAnalyticsSnapshotRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class KpiDashboardMetricsQueryServiceImpl implements KpiDashboardMetricsQueryService {

    private final KpiLiveMetricsCache liveMetricsCache;
    private final AqiCalculationDomainService aqiCalculationDomainService;
    private final TrendAnalysisDomainService trendAnalysisDomainService;
    private final DeviceAnalyticsSnapshotRepository snapshotRepository;

    public KpiDashboardMetricsQueryServiceImpl(
            KpiLiveMetricsCache liveMetricsCache,
            AqiCalculationDomainService aqiCalculationDomainService,
            TrendAnalysisDomainService trendAnalysisDomainService,
            DeviceAnalyticsSnapshotRepository snapshotRepository
    ) {
        this.liveMetricsCache = liveMetricsCache;
        this.aqiCalculationDomainService = aqiCalculationDomainService;
        this.trendAnalysisDomainService = trendAnalysisDomainService;
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KpiDashboardMetrics> handle(GetLiveDashboardMetricsQuery query) {
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
    }
}
