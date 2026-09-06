package com.claircore.analytics.application.internal.queryservices;

import com.claircore.alerting.interfaces.acl.AlertDetails;
import com.claircore.alerting.interfaces.acl.AlertingContextFacade;
import com.claircore.analytics.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.analytics.application.internal.outboundservices.cache.KpiLiveMetricsBuffer;
import com.claircore.analytics.application.internal.outboundservices.cache.LiveMetricsStore;
import com.claircore.analytics.application.queryservices.OverviewDashboardQueryService;
import com.claircore.analytics.domain.model.queries.GetOverviewDashboardQuery;
import com.claircore.analytics.domain.model.valueobjects.AggregatedMetrics;
import com.claircore.analytics.domain.model.valueobjects.DeviceMetricsSnapshot;
import com.claircore.analytics.domain.model.valueobjects.Freshness;
import com.claircore.analytics.domain.model.valueobjects.OverviewDashboardSnapshot;
import com.claircore.analytics.domain.services.MetricsAggregationDomainService;
import com.claircore.analytics.domain.services.TrendAnalysisDomainService;
import com.claircore.analytics.domain.services.AqiCalculationDomainService;
import com.claircore.analytics.domain.repositories.DeviceAnalyticsSnapshotRepository;
import com.claircore.device.interfaces.acl.OrganizationSummary;
import com.claircore.device.interfaces.acl.SpaceSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class OverviewDashboardQueryServiceImpl implements OverviewDashboardQueryService {

    private static final List<String> DEFAULT_ALERT_STATUSES = List.of("ACTIVE", "ACKNOWLEDGED");

    private final ExternalDeviceService externalDeviceService;
    private final AlertingContextFacade alertingContextFacade;
    private final LiveMetricsStore liveMetricsStore;
    private final DeviceAnalyticsSnapshotRepository snapshotRepository;
    private final MetricsAggregationDomainService metricsAggregationDomainService;
    private final TrendAnalysisDomainService trendAnalysisDomainService;
    private final AqiCalculationDomainService aqiCalculationDomainService;

    public OverviewDashboardQueryServiceImpl(
            ExternalDeviceService externalDeviceService,
            AlertingContextFacade alertingContextFacade,
            LiveMetricsStore liveMetricsStore,
            DeviceAnalyticsSnapshotRepository snapshotRepository,
            MetricsAggregationDomainService metricsAggregationDomainService,
            TrendAnalysisDomainService trendAnalysisDomainService,
            AqiCalculationDomainService aqiCalculationDomainService
    ) {
        this.externalDeviceService = externalDeviceService;
        this.alertingContextFacade = alertingContextFacade;
        this.liveMetricsStore = liveMetricsStore;
        this.snapshotRepository = snapshotRepository;
        this.metricsAggregationDomainService = metricsAggregationDomainService;
        this.trendAnalysisDomainService = trendAnalysisDomainService;
        this.aqiCalculationDomainService = aqiCalculationDomainService;
    }

    @Override
    @Transactional(readOnly = true)
    public OverviewDashboardSnapshot handle(GetOverviewDashboardQuery query) {
        UUID ownerUserId = query.ownerUserId();
        int deviceLimit = query.deviceLimitPerSpace();
        int alertLimit = query.alertLimit();

        List<OrganizationSummary> orgs = externalDeviceService.findOrganizationsByOwnerId(ownerUserId);
        
        // Use CompletableFuture to fetch organization details in parallel
        List<CompletableFuture<OverviewDashboardSnapshot.OrganizationBreakdown>> orgBreakdownFutures = new ArrayList<>();
        Set<UUID> allDeviceIds = Collections.synchronizedSet(new HashSet<>());
        
        for (OrganizationSummary org : orgs) {
            CompletableFuture<OverviewDashboardSnapshot.OrganizationBreakdown> orgFuture = CompletableFuture.supplyAsync(() -> {
                List<SpaceSummary> spaces = externalDeviceService.findSpacesByOrganizationId(org.organizationId());
                List<OverviewDashboardSnapshot.SpaceBreakdown> spaceBreakdowns = new ArrayList<>();
                
                // Fetch spaces in parallel
                List<CompletableFuture<OverviewDashboardSnapshot.SpaceBreakdown>> spaceFutures = spaces.stream().map(space -> CompletableFuture.supplyAsync(() -> {
                    List<UUID> spaceDeviceIds = externalDeviceService.findDeviceIdsBySpaceId(space.spaceId(), deviceLimit);
                    allDeviceIds.addAll(spaceDeviceIds);

                    var aggregated = aggregateAcrossDevices(spaceDeviceIds);
                    return new OverviewDashboardSnapshot.SpaceBreakdown(
                            space.spaceId(),
                            space.organizationId(),
                            space.spaceName(),
                            aggregated.aqiValue(),
                            aggregated.aqiCategory(),
                            aggregated.recordedAt(),
                            spaceDeviceIds.size(),
                            aggregated.freshness().name()
                    );
                })).toList();
                
                for (var sf : spaceFutures) {
                    spaceBreakdowns.add(sf.join());
                }

                return new OverviewDashboardSnapshot.OrganizationBreakdown(
                        org.organizationId(),
                        org.organizationName(),
                        spaceBreakdowns
                );
            });
            orgBreakdownFutures.add(orgFuture);
        }

        List<OverviewDashboardSnapshot.OrganizationBreakdown> orgBreakdown = orgBreakdownFutures.stream()
                .map(CompletableFuture::join)
                .toList();

        int spaceCount = orgBreakdown.stream().mapToInt(ob -> ob.spaces().size()).sum();

        // Fetch recent alerts asynchronously
        CompletableFuture<List<OverviewDashboardSnapshot.AlertSummary>> alertsFuture = CompletableFuture.supplyAsync(() -> {
            List<AlertDetails> recentAlerts = alertingContextFacade.getRecentAlertsByOwnerId(ownerUserId, DEFAULT_ALERT_STATUSES, alertLimit);
            return enrichAlertNames(toAlertSummaries(recentAlerts));
        });

        List<OverviewDashboardSnapshot.AlertSummary> alertSummaries = alertsFuture.join();

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
                overall.freshness().name()
        );

        return new OverviewDashboardSnapshot(core, orgBreakdown, alertSummaries, updatedAt);
    }

    private AggregatedMetrics aggregateAcrossDevices(List<UUID> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return new AggregatedMetrics(null, null, null, null, null, null, null, null, null, null, null, Freshness.NO_DATA);
        }

        // Dividir entre dispositivos en caché (LIVE) y los que necesitan Snapshot de la DB
        List<UUID> liveDeviceIds = new ArrayList<>();
        List<UUID> snapshotDeviceIds = new ArrayList<>();
        Map<UUID, DeviceMetricsSnapshot> cachedSnapshots = new HashMap<>();

        for (UUID deviceId : deviceIds) {
            var live = liveMetricsStore.getIfPresent(deviceId);
            if (live != null && !live.isEmpty()) {
                liveDeviceIds.add(deviceId);
                cachedSnapshots.put(deviceId, resolveLiveMetrics(deviceId, live));
            } else {
                snapshotDeviceIds.add(deviceId);
            }
        }

        // Carga por lotes para los que no están en caché
        List<DeviceMetricsSnapshot> allSnapshots = new ArrayList<>(cachedSnapshots.values());
        if (!snapshotDeviceIds.isEmpty()) {
            var latestSnapshots = snapshotRepository.findLatestByDeviceIds(snapshotDeviceIds);
            for (var snapshot : latestSnapshots) {
                allSnapshots.add(new DeviceMetricsSnapshot(
                        DeviceMetricsSnapshot.Source.SNAPSHOT,
                        snapshot.getCalculatedAqi().value(),
                        snapshot.getAverageCo2(),
                        snapshot.getAveragePm2_5(),
                        snapshot.getAverageTemperature(),
                        snapshot.getAverageHumidity(),
                        null, // Delta simplificado para resumen batch
                        null,
                        null,
                        null,
                        snapshot.getTimeWindowEnd()
                ));
            }
        }

        return metricsAggregationDomainService.aggregate(allSnapshots);
    }

    private DeviceMetricsSnapshot resolveLiveMetrics(UUID deviceId, KpiLiveMetricsBuffer live) {
        var avg = live.computeAverages();
        var aqi = aqiCalculationDomainService.calculateAqi(avg.pm2_5(), avg.co2());

        return new DeviceMetricsSnapshot(
                DeviceMetricsSnapshot.Source.LIVE,
                aqi.value(),
                avg.co2(),
                avg.pm2_5(),
                avg.temperature(),
                avg.humidity(),
                null, null, null, null,
                Instant.now()
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
