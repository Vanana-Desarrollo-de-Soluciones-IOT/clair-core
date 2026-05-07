package com.claircore.billing.domain.model.commands;

import com.claircore.billing.domain.model.valueobjects.Money;
import com.claircore.billing.domain.model.valueobjects.UserId;

public record FulfillSubscriptionCommand(
    String stripePaymentIntentId,
    UserId userId,
    Money money
) {
    public FulfillSubscriptionCommand {
        if (stripePaymentIntentId == null || stripePaymentIntentId.isBlank()) {
            throw new IllegalArgumentException("Stripe Payment Intent ID is required");
        }
    }
}
