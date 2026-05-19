package com.claircore.evaluation.interfaces.acl;

import com.claircore.evaluation.domain.model.valueobjects.AirQualityStatus;
import com.claircore.evaluation.domain.model.valueobjects.HealthState;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluationContextFacade {

    Optional<AirQualityStatus> getLatestAirQualityStatus(UUID deviceId);

    Optional<HealthState> getLatestHealthState(UUID deviceId);

    Optional<Instant> getLatestEvaluationRecordedAt(UUID deviceId);

    List<UUID> findDevicesWithCriticalHealth();
}
