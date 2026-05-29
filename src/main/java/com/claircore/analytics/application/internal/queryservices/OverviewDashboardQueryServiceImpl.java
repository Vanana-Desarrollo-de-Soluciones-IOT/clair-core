package com.claircore.analytics.application.internal.queryservices;

import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.interfaces.acl.AlertDetails;
import com.claircore.alerting.interfaces.acl.AlertingContextFacade;
import com.claircore.analytics.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.analytics.application.internal.services.KpiLiveMetricsCache;
import com.claircore.analytics.domain.model.queries.GetOverviewDashboardQuery;
import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;
import com.claircore.analytics.domain.model.valueobjects.OverviewDashboardSnapshot;
import com.claircore.analytics.domain.services.AqiCalculationDomainService;
import com.claircore.analytics.domain.services.OverviewDashboardQueryService;
import com.claircore.analytics.domain.services.TrendAnalysisDomainService;
import com.claircore.analytics.infrastructure.persistence.jpa.repositories.DeviceAnalyticsSnapshotRepository;
import com.claircore.device.interfaces.acl.OrganizationSummary;
import com.claircore.device.interfaces.acl.SpaceSummary;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class OverviewDashboardQueryServiceImpl implements OverviewDashboardQueryService {

    private static final List<AlertStatus> DEFAULT_ALERT_STATUSES = List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED);

    private final ExternalDeviceService externalDeviceService;
    private final AlertingContextFacade alertingContextFacade;
    private final KpiLiveMetricsCache liveMetricsCache;
    private final DeviceAnalyticsSnapshotRepository snapshotRepository;
    private final AqiCalculationDomainService aqiCalculationDomainService;
    private final TrendAnalysisDomainService trendAnalysisDomainService;

    public OverviewDashboardQueryServiceImpl(
            ExternalDeviceService externalDeviceService,
            AlertingContextFacade alertingContextFacade,
            KpiLiveMetricsCache liveMetricsCache,
            DeviceAnalyticsSnapshotRepository snapshotRepository,
            AqiCalculationDomainService aqiCalculationDomainService,
            TrendAnalysisDomainService trendAnalysisDomainService
    ) {
        this.externalDeviceService = externalDeviceService;
        this.alertingContextFacade = alertingContextFacade;
        this.liveMetricsCache = liveMetricsCache;
        this.snapshotRepository = snapshotRepository;
        this.aqiCalculationDomainService = aqiCalculationDomainService;
        this.trendAnalysisDomainService = trendAnalysisDomainService;
    }

    @Override
    @Transactional(readOnly = true)
    public OverviewDashboardSnapshot handle(GetOverviewDashboardQuery query) {
        UUID ownerUserId = query.ownerUserId();
        int deviceLimit = query.deviceLimitPerSpace();
        int alertLimit = query.alertLimit();

        List<OrganizationSummary> orgs = externalDeviceService.findOrganizationsByOwnerId(ownerUserId);
        List<OverviewDashboardSnapshot.OrganizationBreakdown> orgBreakdown = new ArrayList<>();

        Set<UUID> allDeviceIds = new HashSet<>();
        int spaceCount = 0;

        for (OrganizationSummary org : orgs) {
            List<SpaceSummary> spaces = externalDeviceService.findSpacesByOrganizationId(org.organizationId());
            spaceCount += spaces.size();

            List<OverviewDashboardSnapshot.SpaceBreakdown> spaceBreakdowns = new ArrayList<>();
            for (SpaceSummary space : spaces) {
                List<UUID> spaceDeviceIds = externalDeviceService.findDeviceIdsBySpaceId(space.spaceId(), deviceLimit);
                allDeviceIds.addAll(spaceDeviceIds);

                var aggregated = aggregateAcrossDevices(spaceDeviceIds);
                spaceBreakdowns.add(new OverviewDashboardSnapshot.SpaceBreakdown(
                        space.spaceId(),
                        space.organizationId(),
                        space.spaceName(),
                        aggregated.aqiValue(),
                        aggregated.aqiCategory(),
                        aggregated.recordedAt(),
                        spaceDeviceIds.size(),
                        aggregated.freshness()
                ));
            }

            orgBreakdown.add(new OverviewDashboardSnapshot.OrganizationBreakdown(
                    org.organizationId(),
                    org.organizationName(),
                    spaceBreakdowns
            ));
        }

        List<AlertDetails> recentAlerts = alertingContextFacade.getRecentAlertsByOwnerId(ownerUserId, DEFAULT_ALERT_STATUSES, alertLimit);
        List<OverviewDashboardSnapshot.AlertSummary> alertSummaries = toAlertSummaries(recentAlerts);

        // Fill missing names using batch lookups to Device BC (best-effort).
        alertSummaries = enrichAlertNames(alertSummaries);

        var overall = aggregateAcrossDevices(allDeviceIds.stream().toList());
        Instant updatedAt = overall.recordedAt() != null ? overall.recordedAt() : Instant.now();

        var core = new OverviewDashboardSnapshot.OverviewCoreMetrics(
                overall.aqiValue(),
                overall.aqiCategory(),
                overall.averageCo2(),
                overall.averagePm2_5(),
                overall.averageTemperature(),
                overall.averageHumidity(),
                overall.co2DeltaPercentage(),
                overall.pm2_5DeltaPercentage(),
                overall.temperatureDeltaPercentage(),
                overall.humidityDeltaPercentage(),
                overall.recordedAt(),
                orgs.size(),
                spaceCount,
                allDeviceIds.size(),
                overall.freshness()
        );

        return new OverviewDashboardSnapshot(core, orgBreakdown, alertSummaries, updatedAt);
    }

    private record AggregatedMetrics(
            Integer aqiValue,
            String aqiCategory,
            Double averageCo2,
            Double averagePm2_5,
            Double averageTemperature,
            Double averageHumidity,
            Double co2DeltaPercentage,
            Double pm2_5DeltaPercentage,
            Double temperatureDeltaPercentage,
            Double humidityDeltaPercentage,
            Instant recordedAt,
            String freshness
    ) {}

    private AggregatedMetrics aggregateAcrossDevices(List<UUID> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return new AggregatedMetrics(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "NO_DATA"
            );
        }

        List<Double> co2 = new ArrayList<>();
        List<Double> pm25 = new ArrayList<>();
        List<Double> temp = new ArrayList<>();
        List<Double> hum = new ArrayList<>();
        List<Integer> aqi = new ArrayList<>();
        List<Double> co2Delta = new ArrayList<>();
        List<Double> pm25Delta = new ArrayList<>();
        List<Double> tempDelta = new ArrayList<>();
        List<Double> humDelta = new ArrayList<>();
        Instant latest = null;
        boolean hasLive = false;
        boolean hasSnapshot = false;

        for (UUID deviceId : deviceIds) {
            var snapshot = resolveLatestMetrics(deviceId);
            if (snapshot == null) continue;
            if (snapshot.source == Source.LIVE) hasLive = true;
            if (snapshot.source == Source.SNAPSHOT) hasSnapshot = true;

            if (snapshot.averageCo2 != null && Double.isFinite(snapshot.averageCo2)) co2.add(snapshot.averageCo2);
            if (snapshot.averagePm2_5 != null && Double.isFinite(snapshot.averagePm2_5)) pm25.add(snapshot.averagePm2_5);
            if (snapshot.averageTemperature != null && Double.isFinite(snapshot.averageTemperature)) temp.add(snapshot.averageTemperature);
            if (snapshot.averageHumidity != null && Double.isFinite(snapshot.averageHumidity)) hum.add(snapshot.averageHumidity);
            if (snapshot.aqiValue != null) aqi.add(snapshot.aqiValue);
            if (snapshot.co2DeltaPercentage != null && Double.isFinite(snapshot.co2DeltaPercentage)) co2Delta.add(snapshot.co2DeltaPercentage);
            if (snapshot.pm2_5DeltaPercentage != null && Double.isFinite(snapshot.pm2_5DeltaPercentage)) pm25Delta.add(snapshot.pm2_5DeltaPercentage);
            if (snapshot.temperatureDeltaPercentage != null && Double.isFinite(snapshot.temperatureDeltaPercentage)) tempDelta.add(snapshot.temperatureDeltaPercentage);
            if (snapshot.humidityDeltaPercentage != null && Double.isFinite(snapshot.humidityDeltaPercentage)) humDelta.add(snapshot.humidityDeltaPercentage);

            if (snapshot.recordedAt != null) {
                latest = (latest == null || snapshot.recordedAt.isAfter(latest)) ? snapshot.recordedAt : latest;
            }
        }

        Double avgCo2 = average(co2);
        Double avgPm25 = average(pm25);
        Double avgTemp = average(temp);
        Double avgHum = average(hum);
        Double avgCo2Delta = average(co2Delta);
        Double avgPm25Delta = average(pm25Delta);
        Double avgTempDelta = average(tempDelta);
        Double avgHumDelta = average(humDelta);

        Integer avgAqi = averageInt(aqi);
        String aqiCategory = null;
        if (avgAqi != null && avgPm25 != null && avgCo2 != null) {
            try {
                AirQualityIndex derived = aqiCalculationDomainService.calculateAqi(avgPm25, avgCo2);
                avgAqi = derived.value();
                aqiCategory = derived.category().name();
            } catch (Exception ignored) {
                // Keep best-effort values.
            }
        }

        String freshness;
        if (hasLive) freshness = "LIVE";
        else if (hasSnapshot) freshness = "STALE";
        else freshness = "NO_DATA";

        if (avgAqi == null && avgCo2 == null && avgPm25 == null && avgTemp == null && avgHum == null) {
            latest = null;
        }

        return new AggregatedMetrics(
                avgAqi,
                aqiCategory,
                round1(avgCo2),
                round1(avgPm25),
                round1(avgTemp),
                round1(avgHum),
                round1(avgCo2Delta),
                round1(avgPm25Delta),
                round1(avgTempDelta),
                round1(avgHumDelta),
                latest,
                freshness
        );
    }

    private static Double average(List<Double> values) {
        if (values == null || values.isEmpty()) return null;
        double sum = 0.0;
        for (double v : values) sum += v;
        return sum / values.size();
    }

    private static Integer averageInt(List<Integer> values) {
        if (values == null || values.isEmpty()) return null;
        long sum = 0L;
        for (int v : values) sum += v;
        return Math.toIntExact(Math.round((double) sum / values.size()));
    }

    private static Double round1(Double value) {
        if (value == null) return null;
        return Math.round(value * 10.0) / 10.0;
    }

    private enum Source { LIVE, SNAPSHOT }

    private static final class DeviceMetricsSnapshot {
        final Source source;
        final Integer aqiValue;
        final Double averageCo2;
        final Double averagePm2_5;
        final Double averageTemperature;
        final Double averageHumidity;
        final Double co2DeltaPercentage;
        final Double pm2_5DeltaPercentage;
        final Double temperatureDeltaPercentage;
        final Double humidityDeltaPercentage;
        final Instant recordedAt;

        private DeviceMetricsSnapshot(
                Source source,
                Integer aqiValue,
                Double averageCo2,
                Double averagePm2_5,
                Double averageTemperature,
                Double averageHumidity,
                Double co2DeltaPercentage,
                Double pm2_5DeltaPercentage,
                Double temperatureDeltaPercentage,
                Double humidityDeltaPercentage,
                Instant recordedAt
        ) {
            this.source = source;
            this.aqiValue = aqiValue;
            this.averageCo2 = averageCo2;
            this.averagePm2_5 = averagePm2_5;
            this.averageTemperature = averageTemperature;
            this.averageHumidity = averageHumidity;
            this.co2DeltaPercentage = co2DeltaPercentage;
            this.pm2_5DeltaPercentage = pm2_5DeltaPercentage;
            this.temperatureDeltaPercentage = temperatureDeltaPercentage;
            this.humidityDeltaPercentage = humidityDeltaPercentage;
            this.recordedAt = recordedAt;
        }
    }

    private DeviceMetricsSnapshot resolveLatestMetrics(UUID deviceId) {
        if (deviceId == null) return null;

        // Prefer live metrics (last ~5 minutes), else fallback to persisted snapshots.
        var live = liveMetricsCache.getIfPresent(deviceId);
        if (live != null && !live.isEmpty()) {
            var avg = live.computeAverages();
            var aqi = aqiCalculationDomainService.calculateAqi(avg.pm2_5(), avg.co2());

            var latestSnapshot = snapshotRepository
                    .findLatestByDeviceId(deviceId, PageRequest.of(0, 1))
                    .stream().findFirst().orElse(null);
            Double prevCo2 = latestSnapshot != null ? latestSnapshot.getAverageCo2() : null;
            Double prevPm25 = latestSnapshot != null ? latestSnapshot.getAveragePm2_5() : null;
            Double prevTemp = latestSnapshot != null ? latestSnapshot.getAverageTemperature() : null;
            Double prevHum = latestSnapshot != null ? latestSnapshot.getAverageHumidity() : null;

            Double co2Delta = trendAnalysisDomainService.calculateTrend(avg.co2(), prevCo2).deltaPercentage();
            Double pm25Delta = trendAnalysisDomainService.calculateTrend(avg.pm2_5(), prevPm25).deltaPercentage();
            Double tempDelta = trendAnalysisDomainService.calculateTrend(avg.temperature(), prevTemp).deltaPercentage();
            Double humDelta = trendAnalysisDomainService.calculateTrend(avg.humidity(), prevHum).deltaPercentage();

            return new DeviceMetricsSnapshot(
                    Source.LIVE,
                    aqi.value(),
                    avg.co2(),
                    avg.pm2_5(),
                    avg.temperature(),
                    avg.humidity(),
                    co2Delta,
                    pm25Delta,
                    tempDelta,
                    humDelta,
                    Instant.now()
            );
        }

        var snapshots = snapshotRepository.findLatestByDeviceId(deviceId, PageRequest.of(0, 2));
        var latestSnapshot = snapshots.stream().findFirst().orElse(null);
        if (latestSnapshot == null) return null;
        var previousSnapshot = snapshots.size() > 1 ? snapshots.get(1) : null;

        Double co2Delta = previousSnapshot != null
                ? trendAnalysisDomainService.calculateTrend(latestSnapshot.getAverageCo2(), previousSnapshot.getAverageCo2()).deltaPercentage()
                : null;
        Double pm25Delta = previousSnapshot != null
                ? trendAnalysisDomainService.calculateTrend(latestSnapshot.getAveragePm2_5(), previousSnapshot.getAveragePm2_5()).deltaPercentage()
                : null;
        Double tempDelta = previousSnapshot != null
                ? trendAnalysisDomainService.calculateTrend(latestSnapshot.getAverageTemperature(), previousSnapshot.getAverageTemperature()).deltaPercentage()
                : null;
        Double humDelta = previousSnapshot != null
                ? trendAnalysisDomainService.calculateTrend(latestSnapshot.getAverageHumidity(), previousSnapshot.getAverageHumidity()).deltaPercentage()
                : null;

        return new DeviceMetricsSnapshot(
                Source.SNAPSHOT,
                latestSnapshot.getCalculatedAqi().value(),
                latestSnapshot.getAverageCo2(),
                latestSnapshot.getAveragePm2_5(),
                latestSnapshot.getAverageTemperature(),
                latestSnapshot.getAverageHumidity(),
                co2Delta,
                pm25Delta,
                tempDelta,
                humDelta,
                latestSnapshot.getTimeWindowEnd()
        );
    }

    private static List<OverviewDashboardSnapshot.AlertSummary> toAlertSummaries(List<AlertDetails> alerts) {
        if (alerts == null || alerts.isEmpty()) return List.of();
        return alerts.stream().map(a -> new OverviewDashboardSnapshot.AlertSummary(
                a.alertId(),
                a.deviceId(),
                a.spaceId(),
                a.deviceName(),
                null,
                a.message(),
                a.severity(),
                a.status(),
                a.occurredAt()
        )).toList();
    }

    private List<OverviewDashboardSnapshot.AlertSummary> enrichAlertNames(List<OverviewDashboardSnapshot.AlertSummary> alerts) {
        if (alerts == null || alerts.isEmpty()) return List.of();

        List<UUID> deviceIds = alerts.stream()
                .map(OverviewDashboardSnapshot.AlertSummary::deviceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<UUID> spaceIds = alerts.stream()
                .map(OverviewDashboardSnapshot.AlertSummary::spaceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, String> deviceNames = externalDeviceService.findDeviceNamesByDeviceIds(deviceIds);
        Map<UUID, String> spaceNames = externalDeviceService.findSpaceNamesBySpaceIds(spaceIds);

        return alerts.stream().map(a -> new OverviewDashboardSnapshot.AlertSummary(
                a.alertId(),
                a.deviceId(),
                a.spaceId(),
                a.deviceName() != null ? a.deviceName() : deviceNames.get(a.deviceId()),
                a.spaceName() != null ? a.spaceName() : spaceNames.get(a.spaceId()),
                a.message(),
                a.severity(),
                a.status(),
                a.occurredAt()
        )).toList();
    }
}
