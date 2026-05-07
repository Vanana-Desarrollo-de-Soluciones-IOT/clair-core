package com.claircore.billing.application.internal.commandservices;

import com.claircore.billing.domain.gateways.PaymentGateway;
import com.claircore.billing.domain.model.aggregates.Subscription;
import com.claircore.billing.domain.model.commands.CreateCheckoutSessionCommand;
import com.claircore.billing.domain.model.commands.CreatePaymentIntentCommand;
import com.claircore.billing.domain.model.commands.FulfillSubscriptionCommand;
import com.claircore.billing.domain.services.SubscriptionCommandService;
import com.claircore.billing.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionCommandServiceImpl implements SubscriptionCommandService {

    private final PaymentGateway paymentGateway;
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionCommandServiceImpl(PaymentGateway paymentGateway, SubscriptionRepository subscriptionRepository) {
        this.paymentGateway = paymentGateway;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    @Transactional
    public String handle(CreateCheckoutSessionCommand command) {
        // Here we could pre-create a PENDING subscription if we want to track intent
        // For now, we'll just return the Stripe URL
        return paymentGateway.createCheckoutSession(command);
    }

    @Override
    @Transactional
    public String handle(CreatePaymentIntentCommand command) {
        var result = paymentGateway.createPaymentIntent(command);
        
        var subscription = new Subscription(
                command.userId(),
                command.money(),
                result.paymentIntentId()
        );
        subscriptionRepository.save(subscription);
        
        return result.clientSecret();
    }

    @Override
    @Transactional
    public void handle(FulfillSubscriptionCommand command) {
        // Stripe webhook calls this. We find or create the subscription and mark it as ACTIVE.
        // Usually, we'd have a way to relate the PaymentIntent back to the original intent/user.
        // Since we didn't save it yet, we might need to fetch more info from Stripe or rely on metadata.
        
        subscriptionRepository.findByStripePaymentIntentId(command.stripePaymentIntentId())
                .ifPresentOrElse(
                        subscription -> {
                            subscription.markAsActive();
                            subscriptionRepository.save(subscription);
                        },
                        () -> {
                            // If not found, we might need to handle the case where the subscription 
                            // is created upon fulfillment (e.g. from webhook metadata)
                            // This depends on the exact flow.
                        }
                );
    }
}
