package com.claircore.evaluation.domain.model.commands;

import com.claircore.evaluation.domain.model.valueobjects.*;

import java.time.Instant;

public record EvaluateTelemetryCommand(
        DeviceId deviceId,
        String deviceTime,
        String uptime,
        AirQuality airQuality,
        ParticulateMatter particulateMatter,
        Connectivity connectivity,
        String status,
        Instant recordedAt
) {
    public EvaluateTelemetryCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (deviceTime == null || deviceTime.isBlank()) {
            throw new IllegalArgumentException("deviceTime must not be null or blank");
        }
        if (uptime == null || uptime.isBlank()) {
            throw new IllegalArgumentException("uptime must not be null or blank");
        }
        if (airQuality == null) {
            throw new IllegalArgumentException("airQuality must not be null");
        }
        if (particulateMatter == null) {
            throw new IllegalArgumentException("particulateMatter must not be null");
        }
        if (connectivity == null) {
            throw new IllegalArgumentException("connectivity must not be null");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be null or blank");
        }
        if (recordedAt == null) {
            throw new IllegalArgumentException("recordedAt must not be null");
        }
    }
}
