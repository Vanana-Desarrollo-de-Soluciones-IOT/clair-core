package com.claircore.billing.interfaces.rest.controllers;

import com.claircore.billing.domain.model.commands.CreateCheckoutSessionCommand;
import com.claircore.billing.domain.model.queries.GetSubscriptionsByUserIdQuery;
import com.claircore.billing.domain.model.valueobjects.Money;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.domain.services.SubscriptionCommandService;
import com.claircore.billing.domain.services.SubscriptionQueryService;
import com.claircore.billing.interfaces.rest.resources.CreateSubscriptionResource;
import com.claircore.billing.interfaces.rest.resources.SubscriptionResource;
import com.claircore.billing.interfaces.rest.transform.SubscriptionResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Billing and Subscription Management Endpoints")
public class SubscriptionController {

    private final SubscriptionCommandService subscriptionCommandService;
    private final SubscriptionQueryService subscriptionQueryService;

    public SubscriptionController(SubscriptionCommandService subscriptionCommandService, SubscriptionQueryService subscriptionQueryService) {
        this.subscriptionCommandService = subscriptionCommandService;
        this.subscriptionQueryService = subscriptionQueryService;
    }

    @PostMapping("/checkout-session")
    @Operation(summary = "Create a Stripe checkout session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody CreateSubscriptionResource resource) {
        var command = new CreateCheckoutSessionCommand(
                new UserId(resource.userId()),
                new Money(resource.amount(), resource.currency()),
                resource.returnUrl()
        );
        String sessionUrl = subscriptionCommandService.handle(command);
        return ResponseEntity.ok(Map.of("checkoutUrl", sessionUrl));
    }

    @PostMapping("/payment-intent")
    @Operation(summary = "Create a Stripe payment intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(@RequestBody CreateSubscriptionResource resource) {
        var command = new com.claircore.billing.domain.model.commands.CreatePaymentIntentCommand(
                new UserId(resource.userId()),
                new Money(resource.amount(), resource.currency())
        );
        String clientSecret = subscriptionCommandService.handle(command);
        return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all subscriptions for a user")
    public ResponseEntity<List<SubscriptionResource>> getSubscriptionsByUserId(@PathVariable String userId) {
        var query = new GetSubscriptionsByUserIdQuery(new UserId(userId));
        var subscriptions = subscriptionQueryService.handle(query);
        var resources = subscriptions.stream()
                .map(SubscriptionResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }
}
