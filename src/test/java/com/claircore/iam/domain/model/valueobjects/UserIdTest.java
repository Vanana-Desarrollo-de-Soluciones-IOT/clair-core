package com.claircore.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class UserIdTest {

    @Test
    void shouldCreateUserIdWhenUuidIsValid() {
        UUID id = UUID.randomUUID();

        UserId userId = new UserId(id);

        assertEquals(id, userId.userId());
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new UserId(null)
        );

        assertEquals("User ID is required", exception.getMessage());
    }
}
