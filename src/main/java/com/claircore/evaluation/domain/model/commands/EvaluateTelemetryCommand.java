package com.claircore.evaluation.domain.model.commands;

import com.claircore.evaluation.domain.model.valueobjects.*;

import java.time.Instant;

public record EvaluateTelemetryCommand(
        DeviceId deviceId,
        Co2Level co2,
        Pm25Level pm25,
        Pm10Level pm10,
        Temperature temperature,
        Humidity humidity,
        Boolean airQualityValid,
        Boolean pmValid,
        String status,
        Integer statusCode,
        Instant recordedAt
) {
    public EvaluateTelemetryCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (co2 == null) {
            throw new IllegalArgumentException("CO2 level must not be null");
        }
        if (pm25 == null) {
            throw new IllegalArgumentException("PM2.5 level must not be null");
        }
        if (pm10 == null) {
            throw new IllegalArgumentException("PM10 level must not be null");
        }
        if (temperature == null) {
            throw new IllegalArgumentException("Temperature must not be null");
        }
        if (humidity == null) {
            throw new IllegalArgumentException("Humidity must not be null");
        }
        if (airQualityValid == null) {
            throw new IllegalArgumentException("Air quality valid flag must not be null");
        }
        if (pmValid == null) {
            throw new IllegalArgumentException("PM valid flag must not be null");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status must not be null or blank");
        }
        if (statusCode == null) {
            throw new IllegalArgumentException("Status code must not be null");
        }
        if (recordedAt == null) {
            throw new IllegalArgumentException("Recorded at must not be null");
        }
    }
}
