package com.claircore.device.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
    UUID id,
    String name,
    UUID ownerUserId,
    Instant createdAt,
    Instant updatedAt
) {}