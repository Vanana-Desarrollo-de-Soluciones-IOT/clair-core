package com.claircore.evaluation.application.internal.inboundservices.acl;

/**
 * Inbound telemetry integration event.
 */
public record TelemetryRecordedIntegrationEvent(
        String deviceId,
        String hardwareId,
        String deviceTime,
        Long uptimeSeconds,
        Double co2,
        Double temperature,
        Double humidity,
        Integer pm1_0,
        Integer pm2_5,
        Integer pm10,
        String wifiStatus,
        String networkName,
        Integer signalStrength,
        String country,
        Integer healthStatus,
        String status,
        String recordedAt,
        String occurredAt
) {
}
