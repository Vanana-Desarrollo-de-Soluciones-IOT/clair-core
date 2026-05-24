package com.claircore.device.interfaces.rest.resources;

import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request to create or update a device threshold. Threshold triggers when telemetry value is greater than the configured value.")
public record UpdateDeviceThresholdRequest(
        @Schema(description = "Metric type for the threshold", example = "PM25")
        @NotNull
        MetricThreshold metric,

        @Schema(description = "Threshold value", example = "60.00")
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal value,

        @Schema(description = "Whether the threshold is enabled", example = "true")
        @NotNull
        Boolean enabled
) {}
