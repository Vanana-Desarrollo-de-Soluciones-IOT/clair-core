package com.claircore.evaluation.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response representing a stored telemetry record")
public record TelemetryEvaluationResponse(
        @Schema(description = "Evaluation ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "Device ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID deviceId,

        @Schema(description = "Device local time", example = "14:30:25")
        String deviceTime,

        @Schema(description = "System uptime", example = "00:00:20")
        String uptime,

        @Schema(description = "Air quality sensor data")
        AirQualityResponse airQuality,

        @Schema(description = "Particulate matter sensor data")
        ParticulateMatterResponse particulateMatter,

        @Schema(description = "WiFi connectivity status")
        ConnectivityResponse connectivity,

        @Schema(description = "Overall device status", example = "Optimal")
        String status,

        @Schema(description = "When the reading was recorded", example = "2026-05-16T22:30:00Z")
        Instant recordedAt,

        @Schema(description = "When the record was created", example = "2026-05-16T22:30:05Z")
        Instant createdAt
) {
    @Schema(description = "Air quality sensor data")
    public record AirQualityResponse(
            Double co2,
            Double temperature,
            Double humidity
    ) {}

    @Schema(description = "Particulate matter sensor data")
    public record ParticulateMatterResponse(
            Integer pm1_0,
            Integer pm2_5,
            Integer pm10
    ) {}

    @Schema(description = "WiFi connectivity status")
    public record ConnectivityResponse(
            String status
    ) {}
}
