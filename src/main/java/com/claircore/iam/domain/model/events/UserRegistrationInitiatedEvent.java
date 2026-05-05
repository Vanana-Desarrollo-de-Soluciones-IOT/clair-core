package com.claircore.iam.domain.model.events;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;

public record UserRegistrationInitiatedEvent(
    RegistrationSessionId sessionId,
    EmailAddress email,
    long timestamp
) {
    public UserRegistrationInitiatedEvent(RegistrationSessionId sessionId, EmailAddress email) {
        this(sessionId, email, System.currentTimeMillis());
    }
}
