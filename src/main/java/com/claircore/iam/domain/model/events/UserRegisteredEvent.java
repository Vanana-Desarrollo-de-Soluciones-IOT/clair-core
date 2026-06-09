package com.claircore.iam.domain.model.events;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class UserRegisteredEvent extends ApplicationEvent {
    private final UUID userId;

    public UserRegisteredEvent(Object source, UUID userId) {
        super(source);
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
