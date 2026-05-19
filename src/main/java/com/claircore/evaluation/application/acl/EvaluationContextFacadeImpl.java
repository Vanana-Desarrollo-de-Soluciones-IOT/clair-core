package com.claircore.evaluation.application.acl;

import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
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
    public Optional<Instant> getLatestEvaluationRecordedAt(UUID deviceId) {
        var query = new GetLatestEvaluationByDeviceQuery(deviceId);
        return telemetryEvaluationQueryService.handle(query)
                .map(TelemetryEvaluation::getRecordedAt);
    }

    @Override
    public List<UUID> findDevicesWithStatusCodeNotZero() {
        return telemetryEvaluationRepository.findAll(PageRequest.of(0, 1000))
                .getContent()
                .stream()
                .filter(e -> e.getStatusCode() != 0)
                .map(e -> e.getDeviceId().value())
                .distinct()
                .toList();
    }
}
