package com.claircore.evaluation.interfaces.rest.resources;

import com.claircore.evaluation.domain.model.valueobjects.AirQualityStatus;
import com.claircore.evaluation.domain.model.valueobjects.HealthState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Response representing an evaluated telemetry record")
public record TelemetryEvaluationResponse(
        @Schema(description = "Evaluation ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "Device ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID deviceId,

        @Schema(description = "CO2 concentration in ppm", example = "420.0")
        Double co2,

        @Schema(description = "PM2.5 concentration in µg/m³", example = "27.0")
        Double pm25,

        @Schema(description = "PM10 concentration in µg/m³", example = "35.0")
        Double pm10,

        @Schema(description = "Temperature in Celsius", example = "24.99")
        Double temperature,

        @Schema(description = "Relative humidity in percent", example = "50.0")
        Double humidity,

        @Schema(description = "Air quality sensor validity", example = "true")
        Boolean airQualityValid,

        @Schema(description = "Particulate matter sensor validity", example = "true")
        Boolean pmValid,

        @Schema(description = "Device status string", example = "Optimal")
        String status,

        @Schema(description = "Device status code", example = "0")
        Integer statusCode,

        @Schema(description = "Computed air quality status")
        AirQualityStatus airQualityStatus,

        @Schema(description = "Computed health state")
        HealthState healthState,

        @Schema(description = "List of threshold breaches detected")
        List<ThresholdBreachResource> thresholdBreaches,

        @Schema(description = "When the reading was recorded", example = "2026-05-16T22:30:00Z")
        Instant recordedAt,

        @Schema(description = "When the evaluation was created", example = "2026-05-16T22:30:05Z")
        Instant createdAt
) {}
