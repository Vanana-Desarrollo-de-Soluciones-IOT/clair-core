package com.claircore.device.domain.model.valueobjects;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;

@Embeddable
public record DeviceMetricThresholdConfiguration(
        @Enumerated(EnumType.STRING)
        MetricThreshold metric,
        BigDecimal value,
        boolean enabled
) {
    public DeviceMetricThresholdConfiguration {
        if (metric == null) throw new IllegalArgumentException("Metric must not be null");
        if (value == null) throw new IllegalArgumentException("Value must not be null");
        if (value.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Value must not be negative");
    }
}
