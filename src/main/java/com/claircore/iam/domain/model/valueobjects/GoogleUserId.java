package com.claircore.iam.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record GoogleUserId(
    String subject
) {
    public GoogleUserId {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Google user subject cannot be null or empty");
        }
    }
}
