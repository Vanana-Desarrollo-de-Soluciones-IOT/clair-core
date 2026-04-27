package com.claircore.iam.domain.model.commands;

public record SignInCommand(
    String email,
    String password
) {
    public SignInCommand {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");
    }
}
