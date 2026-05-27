package com.claircore.alerting.domain.model.events;

import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.MetricType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when a new alert is created.
 */
public record AlertCreatedEvent(
        UUID alertId,
        UUID deviceId,
        MetricType metric,
        AlertSeverity severity,
        BigDecimal thresholdValue,
        BigDecimal actualValue,
        Instant occurredAt
) {
    public AlertCreatedEvent {
        if (alertId == null) {
            throw new IllegalArgumentException("Alert ID must not be null");
        }
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (metric == null) {
            throw new IllegalArgumentException("Metric must not be null");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Severity must not be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at must not be null");
        }
    }
}
