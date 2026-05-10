package com.claircore.iam.domain.model.events;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.OAuthProvider;

import java.time.Instant;
import java.util.UUID;

public record UserAuthenticatedWithGoogleEvent(
    UUID userId,
    EmailAddress email,
    OAuthProvider provider,
    Instant occurredAt
) {
    public UserAuthenticatedWithGoogleEvent {
        if (userId == null) throw new IllegalArgumentException("User ID is required");
        if (email == null) throw new IllegalArgumentException("Email is required");
        if (provider == null) throw new IllegalArgumentException("Provider is required");
        if (occurredAt == null) throw new IllegalArgumentException("OccurredAt is required");
    }

    public UserAuthenticatedWithGoogleEvent(UUID userId, EmailAddress email, OAuthProvider provider) {
        this(userId, email, provider, Instant.now());
    }
}
