package com.claircore.device.domain.model.commands;

import java.util.UUID;

public record UpdateSpaceNameCommand(
    UUID spaceId,
    String name
) {
    public UpdateSpaceNameCommand {
        if (spaceId == null) {
            throw new IllegalArgumentException("Space ID must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be null or blank");
        }
    }
}