package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.PlanType;
import com.claircore.device.domain.model.valueobjects.UserId;

public record CreateOrganizationCommand(
    String name,
    UserId ownerUserId,
    PlanType planType
) {
    public CreateOrganizationCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Organization name must not be null or blank");
        }
        if (ownerUserId == null) {
            throw new IllegalArgumentException("Owner user ID must not be null");
        }
        if (planType == null) {
            throw new IllegalArgumentException("Plan type must not be null");
        }
    }
}