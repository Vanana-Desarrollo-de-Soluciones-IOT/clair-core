package com.claircore.device.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

import java.security.SecureRandom;
import java.util.Base64;

@Embeddable
public record DeviceSecret(String value) {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    public DeviceSecret {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Device secret must not be null or blank");
        }
    }

    public static DeviceSecret generate() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return new DeviceSecret(ENCODER.encodeToString(bytes));
    }
}
