package com.claircore.evaluation.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to store optimized telemetry data received from an edge device")
public record EvaluateTelemetryRequest(
        @Schema(description = "Device identifier", example = "CLAIR-0001")
        @NotBlank String deviceId,

        @Schema(description = "Device local time", example = "14:30:25")
        @NotBlank String timestamp,

        @Schema(description = "System uptime", example = "00:00:20")
        @NotBlank String uptime,

        @Schema(description = "Air quality sensor data")
        @NotNull AirQualityRequest airQuality,

        @Schema(description = "Particulate matter sensor data")
        @NotNull ParticulateMatterRequest particulateMatter,

        @Schema(description = "WiFi connectivity status")
        @NotNull ConnectivityRequest connectivity,

        @Schema(description = "Device location")
        @NotNull LocationRequest location,

        @Schema(description = "Device health status percentage", example = "100")
        @NotNull @Min(0) @Max(100) Integer healthStatus,

        @Schema(description = "Overall device status", example = "Optimal")
        @NotBlank String status,

        @Schema(description = "Optional timestamp override", example = "2026-05-16T22:30:00-05:00")
        String created_at
) {
    @Schema(description = "Air quality sensor data")
    public record AirQualityRequest(
            @Schema(description = "CO2 concentration in ppm", example = "450.0")
            @NotNull Double co2,

            @Schema(description = "Temperature in Celsius", example = "23.5")
            @NotNull Double temperature,

            @Schema(description = "Relative humidity in percent", example = "52.0")
            @NotNull Double humidity
    ) {}

    @Schema(description = "Particulate matter sensor data")
    public record ParticulateMatterRequest(
            @Schema(description = "PM1.0 in µg/m³", example = "5")
            @NotNull Integer pm1_0,

            @Schema(description = "PM2.5 in µg/m³", example = "12")
            @NotNull Integer pm2_5,

            @Schema(description = "PM10 in µg/m³", example = "25")
            @NotNull Integer pm10
    ) {}

    @Schema(description = "WiFi connectivity status")
    public record ConnectivityRequest(
            @Schema(description = "Connection status", example = "connected")
            @NotBlank String status,

            @Schema(description = "WiFi network name", example = "Wokwi-GUEST")
            String network,

            @Schema(description = "Signal strength in dBm", example = "-65")
            Integer signalStrength
    ) {}

    @Schema(description = "Location data")
    public record LocationRequest(
            @Schema(description = "Country", example = "PERU")
            String country
    ) {}
}
