package com.claircore.billing.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record UserId(String userId) {
    public UserId {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID must not be null or empty");
        }
    }
}
