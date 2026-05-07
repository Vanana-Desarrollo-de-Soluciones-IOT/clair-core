package com.claircore.billing.domain.gateways;

import com.claircore.billing.domain.model.commands.CreateCheckoutSessionCommand;

import com.claircore.billing.domain.model.commands.CreatePaymentIntentCommand;
import com.claircore.billing.domain.model.valueobjects.PaymentIntentResult;

public interface PaymentGateway {
    String createCheckoutSession(CreateCheckoutSessionCommand command);
    PaymentIntentResult createPaymentIntent(CreatePaymentIntentCommand command);
}
