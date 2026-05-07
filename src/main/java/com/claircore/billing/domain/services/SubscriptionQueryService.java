package com.claircore.billing.domain.services;

import com.claircore.billing.domain.model.aggregates.Subscription;
import com.claircore.billing.domain.model.queries.GetSubscriptionByIdQuery;
import com.claircore.billing.domain.model.queries.GetSubscriptionsByUserIdQuery;

import java.util.List;
import java.util.Optional;

public interface SubscriptionQueryService {
    Optional<Subscription> handle(GetSubscriptionByIdQuery query);
    List<Subscription> handle(GetSubscriptionsByUserIdQuery query);
}
