package com.claircore.alerting.domain.model.commands;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Intention to evaluate a telemetry reading against configured thresholds
 * and create/resolve alerts accordingly.
 */
public record EvaluateTelemetryForAlertsCommand(
        UUID deviceId,
        Instant occurredAt,
        BigDecimal pm25,
        BigDecimal co2,
        BigDecimal temperature,
        BigDecimal humidity
) {
    public EvaluateTelemetryForAlertsCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at must not be null");
        }
        if (pm25 == null || co2 == null || temperature == null || humidity == null) {
            throw new IllegalArgumentException("Telemetry values must not be null");
        }
    }
}
