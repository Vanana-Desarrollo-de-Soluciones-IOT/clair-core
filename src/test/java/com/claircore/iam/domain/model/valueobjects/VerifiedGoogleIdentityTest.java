package com.claircore.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class VerifiedGoogleIdentityTest {

    @Test
    void shouldCreateVerifiedGoogleIdentityWhenAllRequiredValuesArePresent() {
        VerifiedGoogleIdentity identity = new VerifiedGoogleIdentity(
                new EmailAddress("user@example.com"),
                new GoogleUserId("google-subject"),
                true
        );

        assertEquals("user@example.com", identity.email().address());
        assertEquals("google-subject", identity.userId().subject());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsMissing() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new VerifiedGoogleIdentity(null, new GoogleUserId("google-subject"), true)
        );

        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsMissing() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new VerifiedGoogleIdentity(new EmailAddress("user@example.com"), null, true)
        );

        assertEquals("User ID is required", exception.getMessage());
    }
}
