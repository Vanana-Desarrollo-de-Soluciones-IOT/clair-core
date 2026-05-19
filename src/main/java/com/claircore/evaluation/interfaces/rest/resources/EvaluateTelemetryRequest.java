package com.claircore.evaluation.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "Request to evaluate telemetry data received from an edge device")
public record EvaluateTelemetryRequest(
        @Schema(description = "Device uptime timestamp in milliseconds", example = "20041")
        @NotNull Long deviceTimestamp,

        @Schema(description = "System uptime in seconds", example = "30")
        @NotNull Integer uptimeSeconds,

        @Schema(description = "CO2 concentration in ppm", example = "420.0")
        @NotNull Double co2,

        @Schema(description = "PM2.5 concentration in µg/m³", example = "27.0")
        @NotNull Double pm25,

        @Schema(description = "PM10 concentration in µg/m³", example = "35.0")
        @NotNull Double pm10,

        @Schema(description = "Temperature in Celsius", example = "24.99")
        @NotNull Double temperature,

        @Schema(description = "Relative humidity in percent", example = "50.0")
        @NotNull Double humidity,

        @Schema(description = "Whether air quality sensor data is valid", example = "true")
        @NotNull Boolean airQualityValid,

        @Schema(description = "Whether particulate matter sensor data is valid", example = "true")
        @NotNull Boolean pmValid,

        @Schema(description = "Overall device status", example = "Optimal")
        @NotBlank String status,

        @Schema(description = "Numeric status code", example = "0")
        @NotNull Integer statusCode,

        @Schema(description = "UTC timestamp when the reading was recorded", example = "2026-05-16T22:30:00Z")
        Instant recordedAt
) {}
