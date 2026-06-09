package com.claircore.iam.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record Password(
    String passwordHash
) {
    public Password {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
    }
}
