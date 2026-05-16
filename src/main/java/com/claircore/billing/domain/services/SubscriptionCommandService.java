package com.claircore.billing.domain.services;

import com.claircore.billing.domain.model.commands.CreateCheckoutSessionCommand;
import com.claircore.billing.domain.model.commands.CreatePaymentIntentCommand;
import com.claircore.billing.domain.model.commands.FulfillSubscriptionCommand;

public interface SubscriptionCommandService {
    String handle(CreateCheckoutSessionCommand command);
    String handle(CreatePaymentIntentCommand command);
    void handle(FulfillSubscriptionCommand command);
    void handle(com.claircore.billing.domain.model.commands.DowngradeToFreemiumCommand command);
}
