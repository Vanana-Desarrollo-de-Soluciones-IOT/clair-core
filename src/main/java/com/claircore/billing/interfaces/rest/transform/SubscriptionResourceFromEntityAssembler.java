package com.claircore.billing.interfaces.rest.transform;

import com.claircore.billing.domain.model.aggregates.PaymentRecord;
import com.claircore.billing.interfaces.rest.resources.SubscriptionResource;

public class SubscriptionResourceFromEntityAssembler {
    public static SubscriptionResource toResourceFromEntity(PaymentRecord entity) {
        return new SubscriptionResource(
                entity.getId().toString(),
                entity.getUserId().userId().toString(),
                entity.getAmount().amount(),
                entity.getAmount().currency(),
                entity.getStatus().name(),
                entity.getStripePaymentIntentId()
        );
    }
}
