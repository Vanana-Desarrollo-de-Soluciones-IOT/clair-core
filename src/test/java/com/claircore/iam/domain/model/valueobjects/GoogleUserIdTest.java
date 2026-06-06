package com.claircore.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GoogleUserIdTest {

    @Test
    void shouldCreateGoogleUserIdWhenSubjectIsValid() {
        GoogleUserId googleUserId = new GoogleUserId("google-subject");

        assertEquals("google-subject", googleUserId.subject());
    }

    @Test
    void shouldThrowExceptionWhenGoogleUserIdIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GoogleUserId(" ")
        );

        assertEquals("Google user subject cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenGoogleUserIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GoogleUserId(null)
        );

        assertEquals("Google user subject cannot be null or empty", exception.getMessage());
    }
}
