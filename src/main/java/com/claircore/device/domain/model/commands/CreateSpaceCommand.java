package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.UserId;

public record CreateSpaceCommand(
    String name,
    UserId ownerUserId
) {
    public CreateSpaceCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Space name must not be null or blank");
        }
        if (ownerUserId == null) {
            throw new IllegalArgumentException("Owner user ID must not be null");
        }
    }
}