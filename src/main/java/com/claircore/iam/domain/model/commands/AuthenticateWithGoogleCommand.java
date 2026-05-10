package com.claircore.iam.domain.model.commands;

import com.claircore.iam.domain.model.valueobjects.GoogleIdToken;

public record AuthenticateWithGoogleCommand(
    GoogleIdToken idToken
) {
    public AuthenticateWithGoogleCommand {
        if (idToken == null) {
            throw new IllegalArgumentException("Google ID token is required");
        }
    }
}
