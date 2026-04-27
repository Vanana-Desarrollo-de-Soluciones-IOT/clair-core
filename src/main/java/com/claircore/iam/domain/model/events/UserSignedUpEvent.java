package com.claircore.iam.domain.model.events;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;

import java.util.UUID;

public record UserSignedUpEvent(
    UUID userId,
    EmailAddress email,
    long timestamp
) {
    public UserSignedUpEvent(UUID userId, EmailAddress email) {
        this(userId, email, System.currentTimeMillis());
    }
}
