package com.claircore.analytics.application.acl;

import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;
import com.claircore.analytics.infrastructure.persistence.jpa.repositories.DeviceAnalyticsSnapshotRepository;
import com.claircore.analytics.interfaces.acl.AnalyticsContextFacade;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AnalyticsContextFacadeImpl implements AnalyticsContextFacade {

    private final DeviceAnalyticsSnapshotRepository snapshotRepository;

    public AnalyticsContextFacadeImpl(DeviceAnalyticsSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    public Optional<AirQualityIndex> getLatestAqiByDeviceId(UUID deviceId) {
        var results = snapshotRepository.findLatestByDeviceId(deviceId, PageRequest.of(0, 1));
        return results.stream().findFirst().map(s -> s.getCalculatedAqi());
    }

    @Override
    public Optional<Instant> getLatestSnapshotTimeByDeviceId(UUID deviceId) {
        var results = snapshotRepository.findLatestByDeviceId(deviceId, PageRequest.of(0, 1));
        return results.stream().findFirst().map(s -> s.getTimeWindowEnd());
    }
}
