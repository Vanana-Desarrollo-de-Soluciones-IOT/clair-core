package com.claircore.device.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

import java.security.SecureRandom;
import java.util.Base64;

@Embeddable
public record ClaimToken(String value) {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    public ClaimToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Claim token must not be null or blank");
        }
    }

    public static ClaimToken generate() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return new ClaimToken(ENCODER.encodeToString(bytes));
    }
}
