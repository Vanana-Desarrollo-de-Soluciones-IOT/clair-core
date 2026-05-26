package com.claircore.alerting.domain.model.queries;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

public record GetAlertsBySpaceQuery(
        UUID spaceId,
        Pageable pageable
) {
    public GetAlertsBySpaceQuery {
        if (spaceId == null) {
            throw new IllegalArgumentException("Space ID must not be null");
        }
    }
}
