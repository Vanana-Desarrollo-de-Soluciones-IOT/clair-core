package com.claircore.alerting.application.internal.outboundservices.acl;

import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Integration event emitted by clair-core for edge consumption.
 *
 * Edge/Embedded uses it to trigger local UX (LED/buzzer/screen) when an incident opens,
 * and to stop it when the incident closes.
 */
public record AlertIncidentChangedIntegrationEvent(
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
