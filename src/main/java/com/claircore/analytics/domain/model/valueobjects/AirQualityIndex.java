package com.claircore.analytics.domain.model.valueobjects;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public record AirQualityIndex(
        Integer value,
        @Enumerated(EnumType.STRING)
        AqiCategory category
) {
    public AirQualityIndex {
        if (value == null || value < 0 || value > 500) {
            throw new IllegalArgumentException("AQI value must be between 0 and 500");
        }
        if (category == null) {
            throw new IllegalArgumentException("AQI category must not be null");
        }
    }
}
