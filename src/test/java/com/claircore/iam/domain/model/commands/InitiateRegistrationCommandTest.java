package com.claircore.iam.domain.model.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class InitiateRegistrationCommandTest {

    @Test
    void shouldCreateInitiateRegistrationCommandWhenValuesAreValid() {
        InitiateRegistrationCommand command = new InitiateRegistrationCommand("user@example.com", "SecurePass123!");

        assertEquals("user@example.com", command.email());
        assertEquals("SecurePass123!", command.password());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new InitiateRegistrationCommand(" ", "SecurePass123!")
        );

        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenPasswordIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new InitiateRegistrationCommand("user@example.com", " ")
        );

        assertEquals("Password is required", exception.getMessage());
    }
}
