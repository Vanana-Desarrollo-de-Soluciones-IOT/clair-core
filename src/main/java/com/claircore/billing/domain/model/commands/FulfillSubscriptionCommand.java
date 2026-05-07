package com.claircore.billing.domain.model.commands;

public record FulfillSubscriptionCommand(String stripePaymentIntentId) {
    public FulfillSubscriptionCommand {
        if (stripePaymentIntentId == null || stripePaymentIntentId.isBlank()) {
            throw new IllegalArgumentException("Stripe Payment Intent ID is required");
        }
    }
}
