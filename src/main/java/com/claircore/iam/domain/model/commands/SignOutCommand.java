package com.claircore.iam.domain.model.commands;

import com.claircore.iam.domain.model.valueobjects.UserId;

public record SignOutCommand(
    UserId userId
) {
    public SignOutCommand {
        if (userId == null) throw new IllegalArgumentException("User ID is required");
    }
}
