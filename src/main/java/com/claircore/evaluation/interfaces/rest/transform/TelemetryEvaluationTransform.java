package com.claircore.evaluation.interfaces.rest.transform;

import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import com.claircore.evaluation.interfaces.rest.resources.ThresholdBreachResource;
import com.claircore.evaluation.interfaces.rest.resources.TelemetryEvaluationResponse;

public class TelemetryEvaluationTransform {

    private TelemetryEvaluationTransform() {}

    public static TelemetryEvaluationResponse toResponse(TelemetryEvaluation evaluation) {
        var breaches = evaluation.getThresholdBreaches().stream()
                .map(b -> new ThresholdBreachResource(b.metric(), b.value(), b.threshold()))
                .toList();

        return new TelemetryEvaluationResponse(
                evaluation.getId(),
                evaluation.getDeviceId().value(),
                evaluation.getCo2().value(),
                evaluation.getPm25().value(),
                evaluation.getPm10().value(),
                evaluation.getTemperature().value(),
                evaluation.getHumidity().value(),
                evaluation.getAirQualityValid(),
                evaluation.getPmValid(),
                evaluation.getStatus(),
                evaluation.getStatusCode(),
                evaluation.getAirQualityStatus(),
                evaluation.getHealthState(),
                breaches,
                evaluation.getRecordedAt(),
                evaluation.getAuditFields().getCreatedAt() != null
                        ? evaluation.getAuditFields().getCreatedAt().toInstant()
                        : null
        );
    }
}
