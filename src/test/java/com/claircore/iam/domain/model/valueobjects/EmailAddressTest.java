package com.claircore.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class EmailAddressTest {

    @Test
    void shouldCreateEmailAddressWhenAddressIsValid() {
        EmailAddress emailAddress = new EmailAddress("user@example.com");

        assertEquals("user@example.com", emailAddress.address());
    }

    @Test
    void shouldThrowExceptionWhenEmailAddressIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new EmailAddress(" ")
        );

        assertEquals("Email address cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmailAddressIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new EmailAddress(null)
        );

        assertEquals("Email address cannot be null or empty", exception.getMessage());
    }
}
