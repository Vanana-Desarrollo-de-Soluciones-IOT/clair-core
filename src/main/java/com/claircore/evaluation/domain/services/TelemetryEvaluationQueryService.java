package com.claircore.evaluation.domain.services;

import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.queries.GetEvaluationsByDeviceQuery;
import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface TelemetryEvaluationQueryService {
    Page<TelemetryEvaluation> handle(GetEvaluationsByDeviceQuery query);
    Optional<TelemetryEvaluation> handle(GetLatestEvaluationByDeviceQuery query);
}
