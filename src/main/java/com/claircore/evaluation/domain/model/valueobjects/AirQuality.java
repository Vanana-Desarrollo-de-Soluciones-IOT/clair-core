package com.claircore.evaluation.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record AirQuality(
        Double co2,
        Double temperature,
        Double humidity,
        Boolean valid
) {
    public AirQuality {
        if (co2 == null) {
            throw new IllegalArgumentException("co2 must not be null");
        }
        if (temperature == null) {
            throw new IllegalArgumentException("temperature must not be null");
        }
        if (humidity == null) {
            throw new IllegalArgumentException("humidity must not be null");
        }
        if (valid == null) {
            throw new IllegalArgumentException("valid must not be null");
        }
    }
}
