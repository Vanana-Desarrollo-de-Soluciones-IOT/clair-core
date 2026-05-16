package com.claircore.iam.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record UserId(@Column(name = "user_id", nullable = false) UUID userId) {
    public UserId {
        if (userId == null) throw new IllegalArgumentException("User ID is required");
    }
}
