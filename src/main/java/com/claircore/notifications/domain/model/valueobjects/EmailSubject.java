package com.claircore.notifications.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record EmailSubject(@Column(name = "subject", nullable = false) String value) {
    public EmailSubject {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Email subject is required");
        if (value.length() > 255) throw new IllegalArgumentException("Email subject cannot exceed 255 characters");
    }
}
