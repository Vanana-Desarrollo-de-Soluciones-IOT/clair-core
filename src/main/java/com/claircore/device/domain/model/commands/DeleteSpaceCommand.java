package com.claircore.device.domain.model.commands;

import java.util.UUID;

public record DeleteSpaceCommand(UUID spaceId) {
    public DeleteSpaceCommand {
        if (spaceId == null) {
            throw new IllegalArgumentException("Space ID must not be null");
        }
    }
}