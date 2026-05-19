package com.claircore.evaluation.domain.model.events;

import java.time.Instant;
import java.util.UUID;

public record ThresholdExceededEvent(
        UUID evaluationId,
        UUID deviceId,
        String metric,
        Double value,
        Double threshold,
        Instant occurredOn
) {
    public ThresholdExceededEvent {
        if (evaluationId == null) {
            throw new IllegalArgumentException("Evaluation ID must not be null");
        }
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (metric == null || metric.isBlank()) {
            throw new IllegalArgumentException("Metric must not be null or blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null");
        }
        if (threshold == null) {
            throw new IllegalArgumentException("Threshold must not be null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("Occurred on must not be null");
        }
    }
}
