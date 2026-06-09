package com.claircore.device.domain.model.commands;

import java.util.UUID;

public record UpdateOrganizationNameCommand(
    UUID organizationId,
    String name
) {
    public UpdateOrganizationNameCommand {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be null or blank");
        }
    }
}