package com.claircore.alerting.application.internal.inboundservices.acl;

/**
 * Integration event emitted by Edge/Embedded to report a condition state change for a metric.
 *
 * Payload is expected to be snake_case.
 */
public record AlertConditionStateChangedIntegrationEvent(
        String deviceId,
        String hardwareId,
        String metric,
        String conditionState,
        String occurredAt,
        String recordedAt
) {
}
