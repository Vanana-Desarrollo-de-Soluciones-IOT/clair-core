package com.claircore.billing.interfaces.rest.resources;

public record SubscriptionResource(
    String id,
    String userId,
    Long amount,
    String currency,
    String status,
    String stripePaymentIntentId
) {}
