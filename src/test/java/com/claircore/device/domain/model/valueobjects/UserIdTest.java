package com.claircore.device.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class UserIdTest {

    @Test
    void shouldCreateUserIdWhenValueIsValid() {
        UUID value = UUID.fromString("550e8400-e29b-41d4-a716-446655440100");

        UserId userId = new UserId(value);

        assertEquals(value, userId.userId());
    }

    @Test
    void shouldThrowExceptionWhenValueIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new UserId(null)
        );

        assertEquals("User ID must not be null", exception.getMessage());
    }
}
