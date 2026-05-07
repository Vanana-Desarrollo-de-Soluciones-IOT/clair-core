package com.claircore.billing.domain.model.commands;

import com.claircore.billing.domain.model.valueobjects.Money;
import com.claircore.billing.domain.model.valueobjects.UserId;

public record CreateCheckoutSessionCommand(
    UserId userId,
    Money money,
    String returnUrl
) {
    public CreateCheckoutSessionCommand {
        if (userId == null) throw new IllegalArgumentException("UserId is required");
        if (money == null) throw new IllegalArgumentException("Money is required");
        if (returnUrl == null || returnUrl.isBlank()) throw new IllegalArgumentException("ReturnUrl is required");
    }
}
