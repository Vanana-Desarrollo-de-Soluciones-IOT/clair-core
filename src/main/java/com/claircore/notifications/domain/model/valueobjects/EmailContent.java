package com.claircore.notifications.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record EmailContent(@Column(name = "content", columnDefinition = "TEXT") String html) {
    public EmailContent {
        if (html == null || html.isBlank()) throw new IllegalArgumentException("Email content is required");
    }
}
