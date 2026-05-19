package com.claircore.evaluation.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record Co2Level(Double value) {
    public Co2Level {
        if (value == null) {
            throw new IllegalArgumentException("CO2 level must not be null");
        }
        if (value < 0 || value > 5000) {
            throw new IllegalArgumentException("CO2 level must be between 0 and 5000 ppm");
        }
    }
}
