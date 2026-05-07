package com.claircore.billing.interfaces.rest.transform;

import com.claircore.billing.domain.model.aggregates.Subscription;
import com.claircore.billing.interfaces.rest.resources.SubscriptionResource;

public class SubscriptionResourceFromEntityAssembler {
    public static SubscriptionResource toResourceFromEntity(Subscription entity) {
        return new SubscriptionResource(
                entity.getId().toString(),
                entity.getUserId().userId(),
                entity.getAmount().amount(),
                entity.getAmount().currency(),
                entity.getStatus().name(),
                entity.getStripePaymentIntentId()
        );
    }
}
