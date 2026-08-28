package com.claircore.alerting.domain.model.events;

import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain Event representing a change in an alert incident.
 */
public record AlertIncidentChangedEvent(
        UUID alertId,
        UUID deviceId,
        String hardwareId,
        UUID spaceId,
        MetricType metric,
        BigDecimal thresholdValue,
        BigDecimal actualValue,
        String message,
        AlertStatus status,
        Instant occurredAt,
        Instant resolvedAt
) {
}
