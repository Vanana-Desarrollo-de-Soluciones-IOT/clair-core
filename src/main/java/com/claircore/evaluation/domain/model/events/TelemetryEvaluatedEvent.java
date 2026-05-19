package com.claircore.evaluation.domain.model.events;

import com.claircore.evaluation.domain.model.valueobjects.AirQualityStatus;
import com.claircore.evaluation.domain.model.valueobjects.HealthState;

import java.time.Instant;
import java.util.UUID;

public record TelemetryEvaluatedEvent(
        UUID evaluationId,
        UUID deviceId,
        AirQualityStatus airQualityStatus,
        HealthState healthState,
        Instant occurredOn
) {
    public TelemetryEvaluatedEvent {
        if (evaluationId == null) {
            throw new IllegalArgumentException("Evaluation ID must not be null");
        }
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (airQualityStatus == null) {
            throw new IllegalArgumentException("Air quality status must not be null");
        }
        if (healthState == null) {
            throw new IllegalArgumentException("Health state must not be null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("Occurred on must not be null");
        }
    }
}
