package com.claircore.iam.domain.model.events;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class UserRegisteredEventTest {

    @Test
    void shouldCreateUserRegisteredEventWhenUserIdIsProvided() {
        Object source = new Object();
        UUID userId = UUID.randomUUID();

        UserRegisteredEvent event = new UserRegisteredEvent(source, userId);

        assertSame(source, event.getSource());
        assertEquals(userId, event.getUserId());
    }
}
