package com.claircore.evaluation.application.acl;

import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
import com.claircore.evaluation.domain.model.valueobjects.AirQualityStatus;
import com.claircore.evaluation.domain.model.valueobjects.HealthState;
import com.claircore.evaluation.domain.services.TelemetryEvaluationQueryService;
import com.claircore.evaluation.infrastructure.persistence.jpa.repositories.TelemetryEvaluationRepository;
import com.claircore.evaluation.interfaces.acl.EvaluationContextFacade;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EvaluationContextFacadeImpl implements EvaluationContextFacade {

    private final TelemetryEvaluationQueryService telemetryEvaluationQueryService;
    private final TelemetryEvaluationRepository telemetryEvaluationRepository;

    public EvaluationContextFacadeImpl(
            TelemetryEvaluationQueryService telemetryEvaluationQueryService,
            TelemetryEvaluationRepository telemetryEvaluationRepository
    ) {
        this.telemetryEvaluationQueryService = telemetryEvaluationQueryService;
        this.telemetryEvaluationRepository = telemetryEvaluationRepository;
    }

    @Override
    public Optional<AirQualityStatus> getLatestAirQualityStatus(UUID deviceId) {
        var query = new GetLatestEvaluationByDeviceQuery(deviceId);
        return telemetryEvaluationQueryService.handle(query)
                .map(e -> e.getAirQualityStatus());
    }

    @Override
    public Optional<HealthState> getLatestHealthState(UUID deviceId) {
        var query = new GetLatestEvaluationByDeviceQuery(deviceId);
        return telemetryEvaluationQueryService.handle(query)
                .map(e -> e.getHealthState());
    }

    @Override
    public Optional<Instant> getLatestEvaluationRecordedAt(UUID deviceId) {
        var query = new GetLatestEvaluationByDeviceQuery(deviceId);
        return telemetryEvaluationQueryService.handle(query)
                .map(e -> e.getRecordedAt());
    }

    @Override
    public List<UUID> findDevicesWithCriticalHealth() {
        return telemetryEvaluationRepository.findAll(PageRequest.of(0, 1000))
                .getContent()
                .stream()
                .filter(e -> e.getHealthState() == HealthState.CRITICAL)
                .map(e -> e.getDeviceId().value())
                .distinct()
                .toList();
    }
}
