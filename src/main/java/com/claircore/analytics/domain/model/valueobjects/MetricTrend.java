package com.claircore.analytics.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record MetricTrend(
        Double currentValue,
        Double previousValue,
        Double deltaPercentage
) {
    public MetricTrend {
        if (currentValue == null) {
            throw new IllegalArgumentException("currentValue must not be null");
        }
        if (previousValue == null) {
            throw new IllegalArgumentException("previousValue must not be null");
        }
        if (deltaPercentage == null) {
            throw new IllegalArgumentException("deltaPercentage must not be null");
        }
    }
}
