package com.claircore.device.domain.model.commands;

import java.util.UUID;

public record DeleteOrganizationCommand(UUID organizationId) {
    public DeleteOrganizationCommand {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID must not be null");
        }
    }
}