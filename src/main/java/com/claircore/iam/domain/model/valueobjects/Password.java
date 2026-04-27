package com.claircore.iam.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record Password(
    String encryptedPassword
) {
    public Password {
        if (encryptedPassword == null || encryptedPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
    }
}
