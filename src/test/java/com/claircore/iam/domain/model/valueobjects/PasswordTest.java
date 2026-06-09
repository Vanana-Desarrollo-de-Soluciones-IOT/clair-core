package com.claircore.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class PasswordTest {

    @Test
    void shouldCreatePasswordWhenHashIsValid() {
        Password password = new Password("encoded-password");

        assertEquals("encoded-password", password.passwordHash());
    }

    @Test
    void shouldThrowExceptionWhenPasswordIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new Password(" ")
        );

        assertEquals("Password cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenPasswordIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new Password(null)
        );

        assertEquals("Password cannot be null or empty", exception.getMessage());
    }
}
