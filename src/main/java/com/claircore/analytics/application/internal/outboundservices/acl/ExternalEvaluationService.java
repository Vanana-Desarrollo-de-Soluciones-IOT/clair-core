package com.claircore.analytics.application.internal.outboundservices.acl;

import com.claircore.evaluation.interfaces.acl.EvaluationContextFacade;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ExternalEvaluationService {

    private final EvaluationContextFacade evaluationContextFacade;

    public ExternalEvaluationService(EvaluationContextFacade evaluationContextFacade) {
        this.evaluationContextFacade = evaluationContextFacade;
    }

    public List<Map<String, Object>> fetchHourlyTelemetryAggregation(Instant start, Instant end) {
        return evaluationContextFacade.getHourlyTelemetryAggregation(start, end);
    }
}
