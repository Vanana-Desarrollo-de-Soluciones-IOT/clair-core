package com.claircore.evaluation.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record Temperature(Double value) {
    public Temperature {
        if (value == null) {
            throw new IllegalArgumentException("Temperature must not be null");
        }
        if (value < -40 || value > 80) {
            throw new IllegalArgumentException("Temperature must be between -40 and 80 Celsius");
        }
    }
}
