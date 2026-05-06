package com.claircore.iam.domain.model.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record TokenJti(
    String jti
) {
    @JsonCreator
    public TokenJti(@JsonProperty("jti") String jti) {
        this.jti = jti;
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("Token JTI cannot be null or empty");
        }
        try {
            UUID.fromString(jti);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Token JTI must be a valid UUID");
        }
    }

    public static TokenJti generate() {
        return new TokenJti(UUID.randomUUID().toString());
    }
}
