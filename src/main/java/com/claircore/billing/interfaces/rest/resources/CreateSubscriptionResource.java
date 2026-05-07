package com.claircore.billing.interfaces.rest.resources;

public record CreateSubscriptionResource(
    String userId,
    Long amount,
    String currency,
    String returnUrl
) {}
