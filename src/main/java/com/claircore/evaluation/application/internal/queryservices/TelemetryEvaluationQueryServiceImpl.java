package com.claircore.evaluation.application.internal.queryservices;

import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.queries.GetEvaluationsByDeviceQuery;
import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
import com.claircore.evaluation.domain.services.TelemetryEvaluationQueryService;
import com.claircore.evaluation.infrastructure.persistence.jpa.repositories.TelemetryEvaluationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TelemetryEvaluationQueryServiceImpl implements TelemetryEvaluationQueryService {

    private final TelemetryEvaluationRepository telemetryEvaluationRepository;

    public TelemetryEvaluationQueryServiceImpl(TelemetryEvaluationRepository telemetryEvaluationRepository) {
        this.telemetryEvaluationRepository = telemetryEvaluationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TelemetryEvaluation> handle(GetEvaluationsByDeviceQuery query) {
        int page = query.page() != null ? query.page() : 0;
        int size = query.size() != null ? query.size() : 20;
        return telemetryEvaluationRepository.findByDeviceId(query.deviceId(), PageRequest.of(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TelemetryEvaluation> handle(GetLatestEvaluationByDeviceQuery query) {
        var results = telemetryEvaluationRepository.findLatestByDeviceId(query.deviceId(), PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
