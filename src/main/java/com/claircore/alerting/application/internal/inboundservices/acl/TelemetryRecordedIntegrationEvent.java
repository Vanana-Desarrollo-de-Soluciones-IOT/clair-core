package com.claircore.alerting.application.internal.inboundservices.acl;

/**
 * Inbound telemetry integration event.
 */
public record TelemetryRecordedIntegrationEvent(
        String deviceId,
        String hardwareId,
        String deviceTime,
        int uptimeSeconds,
        double co2,
        double temperature,
        double humidity,
        int pm1_0,
        int pm2_5,
        int pm10,
        String wifiStatus,
        String networkName,
        int signalStrength,
        String country,
        int healthStatus,
        String status,
        String recordedAt,
        String occurredAt
) {
}
