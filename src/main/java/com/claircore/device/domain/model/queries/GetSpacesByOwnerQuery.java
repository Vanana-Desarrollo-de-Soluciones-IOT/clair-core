package com.claircore.device.domain.model.queries;

import com.claircore.device.domain.model.valueobjects.UserId;

public record GetSpacesByOwnerQuery(UserId ownerUserId) {
    public GetSpacesByOwnerQuery {
        if (ownerUserId == null) {
            throw new IllegalArgumentException("Owner user ID must not be null");
        }
    }
}