package com.claircore.analytics.application.internal.queryservices;

import com.claircore.analytics.domain.model.queries.GetHistoricalTrendQuery;
import com.claircore.analytics.domain.model.valueobjects.KpiTrendPoint;
import com.claircore.analytics.domain.services.KpiHistoricalTrendQueryService;
import com.claircore.analytics.infrastructure.persistence.jpa.repositories.DeviceAnalyticsSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class KpiHistoricalTrendQueryServiceImpl implements KpiHistoricalTrendQueryService {

    private final DeviceAnalyticsSnapshotRepository snapshotRepository;

    public KpiHistoricalTrendQueryServiceImpl(DeviceAnalyticsSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KpiTrendPoint> handle(GetHistoricalTrendQuery query) {
        Instant start;
        Instant end;

        if (query.startDate() != null && query.endDate() != null) {
            start = query.startDate();
            end = query.endDate();
        } else {
            Instant now = Instant.now();
            end = now;
            if (query.period() != null) {
                start = switch (query.period()) {
                    case DAY -> now.minus(Duration.ofDays(1));
                    case WEEK -> now.minus(Duration.ofDays(7));
                    case MONTH -> now.minus(Duration.ofDays(30));
                };
            } else {
                start = now.minus(Duration.ofDays(1));
            }
        }

        var pageable = query.limit() != null ? org.springframework.data.domain.PageRequest.of(0, query.limit()) : org.springframework.data.domain.Pageable.unpaged();

        var snapshots = snapshotRepository.findByDeviceIdAndTimeWindowStartBetween(
                query.deviceId().value(), start, end, pageable
        );

        return snapshots.stream()
                .map(s -> new KpiTrendPoint(
                        s.getTimeWindowStart(),
                        (double) s.getCalculatedAqi().value(),
                        s.getAverageCo2(),
                        s.getAveragePm2_5(),
                        s.getAverageTemperature(),
                        s.getAverageHumidity()
                ))
                .toList();
    }
}
