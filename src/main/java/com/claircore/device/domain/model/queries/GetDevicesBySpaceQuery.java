package com.claircore.device.domain.model.queries;

import java.util.UUID;

public record GetDevicesBySpaceQuery(
    UUID spaceId,
    Integer page,
    Integer size
) {
    public GetDevicesBySpaceQuery {
        if (spaceId == null) {
            throw new IllegalArgumentException("Space ID must not be null");
        }
        if (page != null && page < 0) {
            throw new IllegalArgumentException("Page must be non-negative");
        }
        if (size != null && size < 1) {
            throw new IllegalArgumentException("Size must be at least 1");
        }
    }
}