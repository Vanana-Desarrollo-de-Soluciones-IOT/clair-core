package com.claircore.evaluation.domain.model.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain Event representing that telemetry has been recorded.
 */
public record TelemetryRecordedEvent(
        UUID deviceId,
        String hardwareId,
        double co2,
        double pm25,
        double temperature,
        double humidity,
        Instant occurredAt,
        Instant recordedAt,
        double pm10,
        double pm100,
        String wifiStatus,
        String networkName,
        int signalStrength,
        String country,
        String healthStatus,
        String status,
        long uptimeSeconds,
        String deviceTime
) {
}
