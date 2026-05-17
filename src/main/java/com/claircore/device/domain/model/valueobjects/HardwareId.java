package com.claircore.device.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record HardwareId(String value) {
    public HardwareId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Hardware ID must not be null or blank");
        }

        // Current supported factory formats.
        // Keep legacy HW-0001 style to avoid breaking existing records.
        if (!value.matches("^(CLAIR|HW)-\\d{4}$")) {
            throw new IllegalArgumentException("Hardware ID must match CLAIR-0001");
        }
    }
}
