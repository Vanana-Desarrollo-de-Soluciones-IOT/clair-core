package com.claircore.iam.domain.model.events;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.OAuthProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class UserAuthenticatedWithGoogleEventTest {

    @Test
    void shouldCreateUserAuthenticatedWithGoogleEventWhenValuesAreValid() {
        Instant occurredAt = Instant.now();
        UUID userId = UUID.randomUUID();

        UserAuthenticatedWithGoogleEvent event = new UserAuthenticatedWithGoogleEvent(
                userId,
                new EmailAddress("user@example.com"),
                OAuthProvider.GOOGLE,
                occurredAt
        );

        assertEquals(userId, event.userId());
        assertEquals("user@example.com", event.email().address());
        assertEquals(OAuthProvider.GOOGLE, event.provider());
        assertEquals(occurredAt, event.occurredAt());
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsMissing() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new UserAuthenticatedWithGoogleEvent(null, new EmailAddress("user@example.com"), OAuthProvider.GOOGLE)
        );

        assertEquals("User ID is required", exception.getMessage());
    }
}
