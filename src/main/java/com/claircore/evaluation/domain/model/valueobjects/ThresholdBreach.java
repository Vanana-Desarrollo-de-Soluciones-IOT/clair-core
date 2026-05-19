package com.claircore.evaluation.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record ThresholdBreach(
        String metric,
        Double value,
        Double threshold
) {
    public ThresholdBreach {
        if (metric == null || metric.isBlank()) {
            throw new IllegalArgumentException("Metric must not be null or blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null");
        }
        if (threshold == null) {
            throw new IllegalArgumentException("Threshold must not be null");
        }
    }
}
