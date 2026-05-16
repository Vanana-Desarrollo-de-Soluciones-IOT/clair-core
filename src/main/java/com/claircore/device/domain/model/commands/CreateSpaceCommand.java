package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

public record CreateSpaceCommand(
    String name,
    UUID organizationId,
    UserId ownerUserId
) {
    public CreateSpaceCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Space name must not be null or blank");
        }
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID must not be null");
        }
        if (ownerUserId == null) {
            throw new IllegalArgumentException("Owner user ID must not be null");
        }
    }
}