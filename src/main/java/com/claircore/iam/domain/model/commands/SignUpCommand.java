package com.claircore.iam.domain.model.commands;

public record SignUpCommand(
    String username,
    String email,
    String password
) {
    public SignUpCommand {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username is required");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");
    }
}
