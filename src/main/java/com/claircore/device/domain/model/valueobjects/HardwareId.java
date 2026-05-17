package com.claircore.device.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record HardwareId(String value) {
    public HardwareId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Hardware ID must not be null or blank");
        }
    }
}
