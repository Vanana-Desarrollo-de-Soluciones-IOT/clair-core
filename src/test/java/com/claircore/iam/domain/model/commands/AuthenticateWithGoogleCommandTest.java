package com.claircore.iam.domain.model.commands;

import com.claircore.iam.domain.model.valueobjects.GoogleIdToken;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class AuthenticateWithGoogleCommandTest {

    @Test
    void shouldCreateAuthenticateWithGoogleCommandWhenTokenIsValid() {
        GoogleIdToken token = new GoogleIdToken("google-token");

        AuthenticateWithGoogleCommand command = new AuthenticateWithGoogleCommand(token);

        assertEquals(token, command.idToken());
    }

    @Test
    void shouldThrowExceptionWhenGoogleIdTokenIsMissing() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new AuthenticateWithGoogleCommand(null)
        );

        assertEquals("Google ID token is required", exception.getMessage());
    }
}
