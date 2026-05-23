package com.claircore.analytics.interfaces.acl;

import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AnalyticsContextFacade {

    Optional<AirQualityIndex> getLatestAqiByDeviceId(UUID deviceId);

    Optional<Instant> getLatestSnapshotTimeByDeviceId(UUID deviceId);
}
