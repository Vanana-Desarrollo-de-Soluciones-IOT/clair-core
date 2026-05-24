package com.claircore.device.interfaces.rest.resources;

import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Schema(description = "Device threshold response. Threshold triggers when telemetry value is greater than the configured value.")
public record DeviceThresholdResponse(
        @Schema(description = "Threshold ID")
        UUID id,

        @Schema(description = "Device ID")
        UUID deviceId,

        @Schema(description = "Metric type", example = "PM25")
        MetricThreshold metric,

        @Schema(description = "Metric label", example = "PM2.5")
        String metricLabel,

        @Schema(description = "Metric unit", example = "µg/m³")
        String metricUnit,

        @Schema(description = "Threshold value", example = "60.00")
        BigDecimal value,

        @Schema(description = "Whether the threshold is enabled")
        boolean enabled,

        @Schema(description = "When the threshold was created")
        Instant createdAt,

        @Schema(description = "When the threshold was last updated")
        Instant updatedAt
) {
    public static DeviceThresholdResponse from(DeviceMetricThresholdConfiguration threshold, UUID deviceId) {
        UUID syntheticId = UUID.nameUUIDFromBytes((deviceId.toString() + ":" + threshold.metric().name()).getBytes(StandardCharsets.UTF_8));

        return new DeviceThresholdResponse(
                syntheticId,
                deviceId,
                threshold.metric(),
                threshold.metric().label(),
                threshold.metric().unit(),
                threshold.value(),
                threshold.enabled(),
                null,
                null
        );
    }
}
