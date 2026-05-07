package com.claircore.billing.interfaces.rest.controllers;

import com.claircore.billing.domain.model.commands.FulfillSubscriptionCommand;
import com.claircore.billing.domain.model.valueobjects.Money;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.domain.services.SubscriptionCommandService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks/stripe")
public class StripeWebhookController {

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final SubscriptionCommandService subscriptionCommandService;

    public StripeWebhookController(SubscriptionCommandService subscriptionCommandService) {
        this.subscriptionCommandService = subscriptionCommandService;
    }

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            if (paymentIntent != null) {
                var userId = paymentIntent.getMetadata().get("userId");
                var amount = paymentIntent.getAmount();
                var currency = paymentIntent.getCurrency();

                subscriptionCommandService.handle(new FulfillSubscriptionCommand(
                        paymentIntent.getId(),
                        new UserId(userId),
                        new Money(amount, currency)
                ));
            }
        }

        return ResponseEntity.ok("Received");
    }
}
