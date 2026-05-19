package com.claircore.evaluation.domain.model.queries;

import com.claircore.evaluation.domain.model.valueobjects.DeviceId;

import java.util.UUID;

public record GetEvaluationsByDeviceQuery(
        UUID deviceId,
        Integer page,
        Integer size
) {
    public GetEvaluationsByDeviceQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
    }
}
