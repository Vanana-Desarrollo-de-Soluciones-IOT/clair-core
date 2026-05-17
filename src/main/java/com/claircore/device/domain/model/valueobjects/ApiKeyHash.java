package com.claircore.device.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record ApiKeyHash(String value) {
    public ApiKeyHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("API key hash must not be null or blank");
        }
    }
}
