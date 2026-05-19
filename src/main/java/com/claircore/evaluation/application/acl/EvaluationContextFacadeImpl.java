package com.claircore.evaluation.application.acl;

import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
import com.claircore.evaluation.domain.services.TelemetryEvaluationQueryService;
import com.claircore.evaluation.interfaces.acl.EvaluationContextFacade;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class EvaluationContextFacadeImpl implements EvaluationContextFacade {

    private final TelemetryEvaluationQueryService telemetryEvaluationQueryService;

    public EvaluationContextFacadeImpl(
            TelemetryEvaluationQueryService telemetryEvaluationQueryService
    ) {
        this.telemetryEvaluationQueryService = telemetryEvaluationQueryService;
    }

    @Override
    public Optional<Instant> getLatestEvaluationRecordedAt(UUID deviceId) {
        var query = new GetLatestEvaluationByDeviceQuery(deviceId);
        return telemetryEvaluationQueryService.handle(query)
                .map(TelemetryEvaluation::getRecordedAt);
    }
}
