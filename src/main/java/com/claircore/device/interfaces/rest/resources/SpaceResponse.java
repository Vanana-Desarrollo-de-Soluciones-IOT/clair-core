package com.claircore.device.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record SpaceResponse(
    UUID id,
    String name,
    UUID organizationId,
    UUID ownerUserId,
    Instant createdAt,
    Instant updatedAt
) {}