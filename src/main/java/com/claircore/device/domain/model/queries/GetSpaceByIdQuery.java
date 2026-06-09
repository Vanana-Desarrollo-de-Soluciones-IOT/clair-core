package com.claircore.device.domain.model.queries;

import java.util.UUID;

public record GetSpaceByIdQuery(UUID spaceId) {
    public GetSpaceByIdQuery {
        if (spaceId == null) {
            throw new IllegalArgumentException("Space ID must not be null");
        }
    }
}