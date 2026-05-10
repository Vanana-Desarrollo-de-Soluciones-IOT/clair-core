package com.claircore.billing.application.internal.commandservices;

import com.claircore.billing.domain.gateways.PaymentGateway;
import com.claircore.billing.domain.model.aggregates.Subscription;
import com.claircore.billing.domain.model.commands.CreateCheckoutSessionCommand;
import com.claircore.billing.domain.model.commands.CreatePaymentIntentCommand;
import com.claircore.billing.domain.model.commands.FulfillSubscriptionCommand;
import com.claircore.billing.domain.services.SubscriptionCommandService;
import com.claircore.billing.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionCommandServiceImpl implements SubscriptionCommandService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionCommandServiceImpl.class);

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
        log.info("Handling FulfillSubscriptionCommand for paymentIntentId: {}", command.stripePaymentIntentId());
        
        subscriptionRepository.findByStripePaymentIntentId(command.stripePaymentIntentId())
                .ifPresentOrElse(
                        subscription -> {
                            log.info("Found subscription with ID: {} in PENDING state. Marking as ACTIVE.", subscription.getId());
                            subscription.markAsActive();
                            subscriptionRepository.save(subscription);
                            log.info("Subscription with ID: {} successfully updated to ACTIVE.", subscription.getId());
                        },
                        () -> {
                            log.warn("Subscription not found for stripePaymentIntentId: {}. Cannot mark as active.", 
                                    command.stripePaymentIntentId());
                        }
                );
    }
}
