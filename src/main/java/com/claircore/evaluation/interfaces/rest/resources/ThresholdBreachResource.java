package com.claircore.evaluation.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single threshold breach detected during evaluation")
public record ThresholdBreachResource(
        @Schema(description = "Metric name", example = "co2")
        String metric,

        @Schema(description = "Observed value", example = "1200.0")
        Double value,

        @Schema(description = "Threshold that was exceeded", example = "1000.0")
        Double threshold
) {}
