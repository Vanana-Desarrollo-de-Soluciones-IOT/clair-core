package com.claircore.evaluation.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record Pm25Level(Double value) {
    public Pm25Level {
        if (value == null) {
            throw new IllegalArgumentException("PM2.5 level must not be null");
        }
        if (value < 0 || value > 500) {
            throw new IllegalArgumentException("PM2.5 level must be between 0 and 500 µg/m³");
        }
    }
}
