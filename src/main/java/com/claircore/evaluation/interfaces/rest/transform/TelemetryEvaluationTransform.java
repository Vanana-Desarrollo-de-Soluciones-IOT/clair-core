package com.claircore.evaluation.interfaces.rest.transform;

import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import com.claircore.evaluation.interfaces.rest.resources.TelemetryEvaluationResponse;

public class TelemetryEvaluationTransform {

    private TelemetryEvaluationTransform() {}

    public static TelemetryEvaluationResponse toResponse(TelemetryEvaluation e) {
        var aq = e.getAirQuality();
        var pm = e.getParticulateMatter();
        var conn = e.getConnectivity();

        return new TelemetryEvaluationResponse(
                e.getId(),
                e.getDeviceId().value(),
                e.getDeviceTime(),
                e.getUptime(),
                new TelemetryEvaluationResponse.AirQualityResponse(
                        aq.co2(), aq.temperature(), aq.humidity()
                ),
                new TelemetryEvaluationResponse.ParticulateMatterResponse(
                        pm.pm1_0(), pm.pm2_5(), pm.pm10()
                ),
                new TelemetryEvaluationResponse.ConnectivityResponse(
                        conn.status()
                ),
                e.getStatus(),
                e.getRecordedAt(),
                e.getAuditFields().getCreatedAt() != null
                        ? e.getAuditFields().getCreatedAt().toInstant()
                        : null
        );
    }
}
