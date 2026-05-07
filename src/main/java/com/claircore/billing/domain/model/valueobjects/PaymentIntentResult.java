package com.claircore.billing.domain.model.valueobjects;

public record PaymentIntentResult(String paymentIntentId, String clientSecret) {
    public PaymentIntentResult {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalArgumentException("Payment Intent ID is required");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("Client Secret is required");
        }
    }
}
