package com.claircore.analytics.application.internal.services;

import com.claircore.analytics.application.internal.outboundservices.acl.ExternalEvaluationService;
import com.claircore.analytics.domain.model.entities.DeviceAnalyticsSnapshot;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.services.AqiCalculationDomainService;
import com.claircore.evaluation.interfaces.acl.HourlyTelemetryAverage;
import com.claircore.analytics.infrastructure.persistence.jpa.repositories.DeviceAnalyticsSnapshotRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SnapshotAggregationScheduler {

    private final ExternalEvaluationService externalEvaluationService;
    private final DeviceAnalyticsSnapshotRepository snapshotRepository;
    private final AqiCalculationDomainService aqiCalculationDomainService;

    public SnapshotAggregationScheduler(
            ExternalEvaluationService externalEvaluationService,
            DeviceAnalyticsSnapshotRepository snapshotRepository,
            AqiCalculationDomainService aqiCalculationDomainService
    ) {
        this.externalEvaluationService = externalEvaluationService;
        this.snapshotRepository = snapshotRepository;
        this.aqiCalculationDomainService = aqiCalculationDomainService;
    }

    @Scheduled(cron = "0 0 * * * *") // cada hora en punto
    @Transactional
    public void aggregateHourlySnapshots() {

        Instant windowEnd = Instant.now().truncatedTo(ChronoUnit.HOURS);
        Instant windowStart = windowEnd.minus(1, ChronoUnit.HOURS);

        List<HourlyTelemetryAverage> rows = externalEvaluationService.fetchHourlyTelemetryAggregation(windowStart, windowEnd);

        for (HourlyTelemetryAverage row : rows) {
            Double avgCo2 = row.averageCo2();
            Double avgPm25 = row.averagePm25();
            Double avgTemp = row.averageTemperature();
            Double avgHum = row.averageHumidity();

            var aqi = aqiCalculationDomainService.calculateAqi(avgPm25, avgCo2);

            var snapshot = new DeviceAnalyticsSnapshot(
                    new DeviceId(row.deviceId()),
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
