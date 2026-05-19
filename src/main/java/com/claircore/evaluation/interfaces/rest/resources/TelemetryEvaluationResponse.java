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

        @Schema(description = "Device uptime timestamp in milliseconds", example = "20041")
        Long deviceTimestamp,

        @Schema(description = "System uptime in seconds", example = "30")
        Integer uptimeSeconds,

        @Schema(description = "Air quality sensor data")
        AirQualityResponse airQuality,

        @Schema(description = "Particulate matter sensor data")
        ParticulateMatterResponse particulateMatter,

        @Schema(description = "WiFi connectivity status")
        ConnectivityResponse connectivity,

        @Schema(description = "Device health metrics")
        DeviceHealthResponse deviceHealth,

        @Schema(description = "Hardware information")
        DeviceInfoResponse deviceInfo,

        @Schema(description = "Overall device status", example = "Optimal")
        String status,

        @Schema(description = "Numeric status code", example = "0")
        Integer statusCode,

        @Schema(description = "When the reading was recorded", example = "2026-05-16T22:30:00Z")
        Instant recordedAt,

        @Schema(description = "When the record was created", example = "2026-05-16T22:30:05Z")
        Instant createdAt
) {
    @Schema(description = "Air quality sensor data")
    public record AirQualityResponse(
            Double co2,
            Double temperature,
            Double humidity,
            Boolean valid
    ) {}

    @Schema(description = "Particulate matter sensor data")
    public record ParticulateMatterResponse(
            Integer pm1_0,
            Integer pm2_5,
            Integer pm10,
            Boolean valid
    ) {}

    @Schema(description = "WiFi connectivity status")
    public record ConnectivityResponse(
            String status,
            String ssid,
            String ip,
            Integer rssi,
            String mac,
            Integer channel
    ) {}

    @Schema(description = "Device health metrics")
    public record DeviceHealthResponse(
            Integer freeHeap,
            Integer minFreeHeap,
            Integer heapSize,
            Integer maxAllocHeap,
            String scd41Status,
            String pms5003Status,
            Integer lastValidAirQualitySec,
            Integer lastValidPMSec
    ) {}

    @Schema(description = "Hardware information")
    public record DeviceInfoResponse(
            String chipModel,
            Integer chipRevision,
            Integer cpuFreqMHz,
            Integer flashSize,
            Integer sketchSize,
            Integer freeSketchSpace
    ) {}
}
