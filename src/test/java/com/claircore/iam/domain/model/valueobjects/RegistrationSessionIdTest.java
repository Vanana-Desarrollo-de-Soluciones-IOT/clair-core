package com.claircore.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class RegistrationSessionIdTest {

    @Test
    void shouldCreateRegistrationSessionIdWhenUuidIsValid() {
        String id = UUID.randomUUID().toString();

        RegistrationSessionId sessionId = new RegistrationSessionId(id);

        assertEquals(id, sessionId.id());
    }

    @Test
    void shouldGenerateValidRegistrationSessionIdWhenFactoryIsUsed() {
        RegistrationSessionId sessionId = RegistrationSessionId.generate();

        assertNotNull(sessionId.id());
        assertEquals(UUID.fromString(sessionId.id()).toString(), sessionId.id());
    }

    @Test
    void shouldThrowExceptionWhenRegistrationSessionIdIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new RegistrationSessionId(" ")
        );

        assertEquals("Registration session ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRegistrationSessionIdIsNotUuid() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new RegistrationSessionId("not-a-uuid")
        );

        assertEquals("Registration session ID must be a valid UUID", exception.getMessage());
    }
}
