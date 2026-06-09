package com.claircore.iam.domain.model.commands;

public record InitiateRegistrationCommand(
    String email,
    String password
) {
    public InitiateRegistrationCommand {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");
    }
}
