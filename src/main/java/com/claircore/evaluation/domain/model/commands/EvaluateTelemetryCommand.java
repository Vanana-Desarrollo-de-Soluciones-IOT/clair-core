package com.claircore.evaluation.domain.model.commands;

import com.claircore.evaluation.domain.model.valueobjects.*;

import java.time.Instant;

public record EvaluateTelemetryCommand(
        DeviceId deviceId,
        Long deviceTimestamp,
        Integer uptimeSeconds,
        AirQuality airQuality,
        ParticulateMatter particulateMatter,
        Connectivity connectivity,
        DeviceHealth deviceHealth,
        DeviceInfo deviceInfo,
        String status,
        Integer statusCode,
        Instant recordedAt
) {
    public EvaluateTelemetryCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (deviceTimestamp == null) {
            throw new IllegalArgumentException("deviceTimestamp must not be null");
        }
        if (uptimeSeconds == null) {
            throw new IllegalArgumentException("uptimeSeconds must not be null");
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
        if (deviceHealth == null) {
            throw new IllegalArgumentException("deviceHealth must not be null");
        }
        if (deviceInfo == null) {
            throw new IllegalArgumentException("deviceInfo must not be null");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be null or blank");
        }
        if (statusCode == null) {
            throw new IllegalArgumentException("statusCode must not be null");
        }
        if (recordedAt == null) {
            throw new IllegalArgumentException("recordedAt must not be null");
        }
    }
}
