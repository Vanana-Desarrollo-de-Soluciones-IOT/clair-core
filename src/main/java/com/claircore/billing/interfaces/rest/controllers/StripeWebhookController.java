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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks/stripe")
@Tag(name = "Webhooks", description = "Webhook Endpoints")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

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
            log.error("Stripe Webhook Signature Verification Failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        log.info("Received Stripe Webhook event: {}", event.getType());

        if ("payment_intent.succeeded".equals(event.getType())) {
            var deserializer = event.getDataObjectDeserializer();
            PaymentIntent paymentIntent = null;
            if (deserializer.getObject().isPresent()) {
                paymentIntent = (PaymentIntent) deserializer.getObject().get();
            } else {
                try {
                    log.info("API version mismatch detected. Attempting unsafe deserialization for payment_intent.succeeded.");
                    paymentIntent = (PaymentIntent) deserializer.deserializeUnsafe();
                } catch (Exception e) {
                    log.error("Failed to deserialize PaymentIntent unsafely", e);
                }
            }

            if (paymentIntent != null) {
                var userId = paymentIntent.getMetadata().get("userId");
                var amount = paymentIntent.getAmount();
                var currency = paymentIntent.getCurrency();

                log.info("Processing payment_intent.succeeded - id: {}, userId: {}, amount: {}", 
                        paymentIntent.getId(), userId, amount);

                try {
                    subscriptionCommandService.handle(new FulfillSubscriptionCommand(
                            paymentIntent.getId(),
                            new UserId(userId),
                            new Money(amount, currency)));
                    log.info("FulfillSubscriptionCommand processed successfully for payment intent id: {}", paymentIntent.getId());
                } catch (Exception e) {
                    log.error("Error handling FulfillSubscriptionCommand: {}", e.getMessage(), e);
                    // We can choose to throw or return bad request depending on retry preference
                }
            } else {
                log.warn("payment_intent.succeeded event had null paymentIntent object");
            }
        }

        return ResponseEntity.ok("Received");
    }
}
