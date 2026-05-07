package com.claircore.billing.domain.model.queries;

import com.claircore.billing.domain.model.valueobjects.UserId;

public record GetSubscriptionsByUserIdQuery(UserId userId) {
    public GetSubscriptionsByUserIdQuery {
        if (userId == null) throw new IllegalArgumentException("UserId is required");
    }
}
