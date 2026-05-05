package com.claircore.iam.domain.model.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record RegistrationSessionId(
    String id
) {
    @JsonCreator
    public RegistrationSessionId(@JsonProperty("id") String id) {
        this.id = id;
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Registration session ID cannot be null or empty");
        }
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Registration session ID must be a valid UUID");
        }
    }

    public static RegistrationSessionId generate() {
        return new RegistrationSessionId(UUID.randomUUID().toString());
    }
}
