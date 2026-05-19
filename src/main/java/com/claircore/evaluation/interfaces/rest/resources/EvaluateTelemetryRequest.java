package com.claircore.evaluation.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "Request to store telemetry data received from an edge device")
public record EvaluateTelemetryRequest(
        @Schema(description = "Device identifier", example = "CLAIR001")
        @NotBlank String deviceId,

        @Schema(description = "Device uptime timestamp in milliseconds", example = "20041")
        @NotNull Long timestamp,

        @Schema(description = "System uptime in seconds", example = "30")
        @NotNull Integer uptime,

        @Schema(description = "Air quality sensor data")
        @NotNull AirQualityRequest airQuality,

        @Schema(description = "Particulate matter sensor data")
        @NotNull ParticulateMatterRequest particulateMatter,

        @Schema(description = "WiFi connectivity status")
        @NotNull ConnectivityRequest connectivity,

        @Schema(description = "Device health metrics")
        @NotNull DeviceHealthRequest deviceHealth,

        @Schema(description = "Hardware information")
        @NotNull DeviceInfoRequest deviceInfo,

        @Schema(description = "Overall device status", example = "Optimal")
        @NotBlank String status,

        @Schema(description = "Numeric status code", example = "0")
        @NotNull Integer statusCode,

        @Schema(description = "Optional timestamp override", example = "2026-05-16T22:30:00-05:00")
        String created_at
) {
    @Schema(description = "Air quality sensor data")
    public record AirQualityRequest(
            @Schema(description = "CO2 concentration in ppm", example = "420.0")
            @NotNull Double co2,

            @Schema(description = "Temperature in Celsius", example = "24.99")
            @NotNull Double temperature,

            @Schema(description = "Relative humidity in percent", example = "50.0")
            @NotNull Double humidity,

            @Schema(description = "Whether sensor data is valid", example = "true")
            @NotNull Boolean valid
    ) {}

    @Schema(description = "Particulate matter sensor data")
    public record ParticulateMatterRequest(
            @Schema(description = "PM1.0 in µg/m³", example = "16")
            @NotNull Integer pm1_0,

            @Schema(description = "PM2.5 in µg/m³", example = "27")
            @NotNull Integer pm2_5,

            @Schema(description = "PM10 in µg/m³", example = "35")
            @NotNull Integer pm10,

            @Schema(description = "Whether sensor data is valid", example = "true")
            @NotNull Boolean valid
    ) {}

    @Schema(description = "WiFi connectivity status")
    public record ConnectivityRequest(
            @Schema(description = "Connection status", example = "connected")
            @NotBlank String status,

            @Schema(description = "Network SSID", example = "Wokwi-GUEST")
            @NotBlank String ssid,

            @Schema(description = "IP address", example = "10.13.37.2")
            @NotBlank String ip,

            @Schema(description = "Signal strength dBm", example = "-80")
            @NotNull Integer rssi,

            @Schema(description = "MAC address", example = "24:0A:C4:00:01:10")
            @NotBlank String mac,

            @Schema(description = "WiFi channel", example = "6")
            @NotNull Integer channel
    ) {}

    @Schema(description = "Device health metrics")
    public record DeviceHealthRequest(
            @Schema(description = "Free heap bytes", example = "241176")
            @NotNull Integer freeHeap,

            @Schema(description = "Minimum free heap bytes", example = "236932")
            @NotNull Integer minFreeHeap,

            @Schema(description = "Total heap size bytes", example = "318968")
            @NotNull Integer heapSize,

            @Schema(description = "Max alloc heap bytes", example = "110580")
            @NotNull Integer maxAllocHeap,

            @Schema(description = "SCD41 sensor status", example = "ok")
            @NotBlank String scd41Status,

            @Schema(description = "PMS5003 sensor status", example = "ok")
            @NotBlank String pms5003Status,

            @Schema(description = "Seconds since last valid air quality", example = "0")
            @NotNull Integer lastValidAirQualitySec,

            @Schema(description = "Seconds since last valid PM reading", example = "0")
            @NotNull Integer lastValidPMSec
    ) {}

    @Schema(description = "Hardware information")
    public record DeviceInfoRequest(
            @Schema(description = "Chip model", example = "ESP32-D0WDQ6-V3")
            @NotBlank String chipModel,

            @Schema(description = "Chip revision", example = "3")
            @NotNull Integer chipRevision,

            @Schema(description = "CPU frequency MHz", example = "240")
            @NotNull Integer cpuFreqMHz,

            @Schema(description = "Flash size bytes", example = "4194304")
            @NotNull Integer flashSize,

            @Schema(description = "Sketch size bytes", example = "978384")
            @NotNull Integer sketchSize,

            @Schema(description = "Free sketch space bytes", example = "0")
            @NotNull Integer freeSketchSpace
    ) {}
}
