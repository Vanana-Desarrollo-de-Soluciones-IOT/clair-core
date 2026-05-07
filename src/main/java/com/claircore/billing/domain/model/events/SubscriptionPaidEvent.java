package com.claircore.billing.domain.model.events;

import com.claircore.billing.domain.model.valueobjects.UserId;
import org.springframework.context.ApplicationEvent;

public class SubscriptionPaidEvent extends ApplicationEvent {
    private final String stripePaymentIntentId;
    private final UserId userId;

    public SubscriptionPaidEvent(Object source, String stripePaymentIntentId, UserId userId) {
        super(source);
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.userId = userId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public UserId getUserId() {
        return userId;
    }
}
