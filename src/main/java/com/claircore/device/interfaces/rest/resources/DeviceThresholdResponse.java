package com.claircore.device.interfaces.rest.resources;

import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.domain.model.valueobjects.ThresholdOperator;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Device threshold response")
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

        @Schema(description = "Comparison operator", example = "GREATER_THAN")
        ThresholdOperator operator,

        @Schema(description = "Operator symbol", example = ">")
        String operatorSymbol,

        @Schema(description = "Threshold value", example = "60.00")
        BigDecimal value,

        @Schema(description = "Whether the threshold is enabled")
        boolean enabled,

        @Schema(description = "When the threshold was created")
        Instant createdAt,

        @Schema(description = "When the threshold was last updated")
        Instant updatedAt
) {
    public static DeviceThresholdResponse from(
            com.claircore.device.domain.model.entities.DeviceThreshold threshold,
            UUID deviceId) {
        return new DeviceThresholdResponse(
                threshold.getId(),
                deviceId,
                threshold.getMetric(),
                threshold.getMetric().label(),
                threshold.getMetric().unit(),
                threshold.getOperator(),
                threshold.getOperator().symbol(),
                threshold.getValue(),
                threshold.isEnabled(),
                threshold.getAuditFields().getCreatedAt() != null ?
                        threshold.getAuditFields().getCreatedAt().toInstant() : null,
                threshold.getAuditFields().getUpdatedAt() != null ?
                        threshold.getAuditFields().getUpdatedAt().toInstant() : null
        );
    }
}