package com.claircore.billing.domain.gateways;

import com.claircore.billing.domain.model.commands.CreateCheckoutSessionCommand;

import com.claircore.billing.domain.model.commands.CreatePaymentIntentCommand;

public interface PaymentGateway {
    String createCheckoutSession(CreateCheckoutSessionCommand command);
    String createPaymentIntent(CreatePaymentIntentCommand command);
}
