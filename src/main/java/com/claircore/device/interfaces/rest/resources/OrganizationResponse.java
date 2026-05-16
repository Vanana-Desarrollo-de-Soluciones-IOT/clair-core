package com.claircore.device.interfaces.rest.resources;

import com.claircore.device.domain.model.valueobjects.PlanType;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
    UUID id,
    String name,
    PlanType planType,
    UUID ownerUserId,
    Instant createdAt,
    Instant updatedAt
) {}