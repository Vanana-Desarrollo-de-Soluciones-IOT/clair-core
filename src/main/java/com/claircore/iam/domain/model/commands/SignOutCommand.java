package com.claircore.iam.domain.model.commands;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;

public record SignOutCommand(
    EmailAddress email
) {
    public SignOutCommand {
        if (email == null) throw new IllegalArgumentException("Email is required");
    }
}
