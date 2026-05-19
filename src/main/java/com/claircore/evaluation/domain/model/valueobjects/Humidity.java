package com.claircore.evaluation.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record Humidity(Double value) {
    public Humidity {
        if (value == null) {
            throw new IllegalArgumentException("Humidity must not be null");
        }
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("Humidity must be between 0 and 100 percent");
        }
    }
}
