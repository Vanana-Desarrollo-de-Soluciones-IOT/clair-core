package com.claircore.alerting.domain.model.queries;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

public record GetAlertsByOwnerQuery(
        UUID ownerUserId,
        Pageable pageable
) {
    public GetAlertsByOwnerQuery {
        if (ownerUserId == null) {
            throw new IllegalArgumentException("Owner user ID must not be null");
        }
        if (pageable == null) {
            throw new IllegalArgumentException("Pageable must not be null");
        }
    }
}
