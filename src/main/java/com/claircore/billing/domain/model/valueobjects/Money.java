package com.claircore.billing.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Money(
    @Column(name = "amount") Long amount,
    @Column(name = "currency") String currency
) {
    public Money {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency must not be empty");
        }
    }
}
