package com.claircore.iam.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record Username(
    String username
) {
    public Username {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
    }
}
