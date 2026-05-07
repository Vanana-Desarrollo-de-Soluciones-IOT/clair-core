package com.claircore.billing.domain.model.commands;

import com.claircore.billing.domain.model.valueobjects.Money;
import com.claircore.billing.domain.model.valueobjects.UserId;

public record CreatePaymentIntentCommand(
    UserId userId,
    Money money
) {
    public CreatePaymentIntentCommand {
        if (userId == null) throw new IllegalArgumentException("UserId is required");
        if (money == null) throw new IllegalArgumentException("Money is required");
    }
}
