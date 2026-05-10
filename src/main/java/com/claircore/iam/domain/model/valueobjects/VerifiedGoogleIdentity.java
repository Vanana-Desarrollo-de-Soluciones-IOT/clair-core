package com.claircore.iam.domain.model.valueobjects;

public record VerifiedGoogleIdentity(
    EmailAddress email,
    GoogleUserId userId,
    boolean emailVerified
) {
    public VerifiedGoogleIdentity {
        if (email == null) throw new IllegalArgumentException("Email is required");
        if (userId == null) throw new IllegalArgumentException("User ID is required");
    }
}
