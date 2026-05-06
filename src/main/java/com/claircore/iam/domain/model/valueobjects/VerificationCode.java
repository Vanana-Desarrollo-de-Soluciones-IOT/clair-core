package com.claircore.iam.domain.model.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;

@Embeddable
public record VerificationCode(
    String code
) {
    @JsonCreator
    public VerificationCode(@JsonProperty("code") String code) {
        this.code = code;
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Verification code cannot be null or empty");
        }
        if (!code.matches("^[A-Z0-9]{4}-[A-Z0-9]{4}$")) {
            throw new IllegalArgumentException("Verification code must be in format XXXX-XXXX (uppercase alphanumeric)");
        }
    }

    public boolean matches(String other) {
        return code.equals(other);
    }
}
