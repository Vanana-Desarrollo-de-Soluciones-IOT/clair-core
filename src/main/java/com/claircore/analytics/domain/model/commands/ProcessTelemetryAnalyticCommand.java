package com.claircore.analytics.domain.model.commands;

import com.claircore.analytics.domain.model.valueobjects.DeviceId;

import java.time.Instant;

public record ProcessTelemetryAnalyticCommand(
        DeviceId deviceId,
        Double co2,
        Double pm2_5,
        Double temperature,
        Double humidity,
        Instant recordedAt
) {
    public ProcessTelemetryAnalyticCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (co2 == null || co2 < 0) {
            throw new IllegalArgumentException("co2 must not be null or negative");
        }
        if (pm2_5 == null || pm2_5 < 0) {
            throw new IllegalArgumentException("pm2_5 must not be null or negative");
        }
        if (temperature == null) {
            throw new IllegalArgumentException("temperature must not be null");
        }
        if (humidity == null) {
            throw new IllegalArgumentException("humidity must not be null");
        }
        if (recordedAt == null) {
            throw new IllegalArgumentException("recordedAt must not be null");
        }
    }
}
