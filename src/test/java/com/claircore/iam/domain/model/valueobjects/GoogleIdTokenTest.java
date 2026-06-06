package com.claircore.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GoogleIdTokenTest {

    @Test
    void shouldCreateGoogleIdTokenWhenTokenIsValid() {
        GoogleIdToken token = new GoogleIdToken("token-value");

        assertEquals("token-value", token.token());
    }

    @Test
    void shouldThrowExceptionWhenGoogleIdTokenIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GoogleIdToken(" ")
        );

        assertEquals("Google ID token cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenGoogleIdTokenIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GoogleIdToken(null)
        );

        assertEquals("Google ID token cannot be null or empty", exception.getMessage());
    }
}
