package com.claircore.billing.domain.model.queries;

import java.util.UUID;

public record GetSubscriptionByIdQuery(UUID subscriptionId) {
    public GetSubscriptionByIdQuery {
        if (subscriptionId == null) throw new IllegalArgumentException("SubscriptionId is required");
    }
}
