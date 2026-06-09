package com.claircore.evaluation.domain.model.queries;

import java.util.UUID;

public record GetLatestEvaluationByDeviceQuery(
        UUID deviceId
) {
    public GetLatestEvaluationByDeviceQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
    }
}
