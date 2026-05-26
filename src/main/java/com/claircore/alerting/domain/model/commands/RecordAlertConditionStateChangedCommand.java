package com.claircore.alerting.domain.model.commands;

import com.claircore.alerting.domain.model.valueobjects.AlertConditionState;
import com.claircore.alerting.domain.model.valueobjects.MetricType;

import java.time.Instant;
import java.util.UUID;

/**
 * Intention to record a metric condition state change as reported by Edge/Embedded.
 */
public record RecordAlertConditionStateChangedCommand(
        UUID deviceId,
        MetricType metric,
        AlertConditionState conditionState,
        Instant occurredAt
) {
    public RecordAlertConditionStateChangedCommand {
        if (deviceId == null) throw new IllegalArgumentException("Device ID must not be null");
        if (metric == null) throw new IllegalArgumentException("Metric must not be null");
        if (conditionState == null) throw new IllegalArgumentException("Condition state must not be null");
        if (occurredAt == null) throw new IllegalArgumentException("Occurred at must not be null");
    }
}
