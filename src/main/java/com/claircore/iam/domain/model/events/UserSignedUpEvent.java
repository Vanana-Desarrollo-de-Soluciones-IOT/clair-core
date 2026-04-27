package com.claircore.iam.domain.model.events;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;

public record UserSignedUpEvent(
    Long userId,
    EmailAddress email,
    long timestamp
) {
    public UserSignedUpEvent(Long userId, EmailAddress email) {
        this(userId, email, System.currentTimeMillis());
    }
}
