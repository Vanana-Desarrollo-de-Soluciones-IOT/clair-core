package com.claircore.billing.domain.model.aggregates;

import com.claircore.billing.domain.model.events.SubscriptionPaidEvent;
import com.claircore.billing.domain.model.valueobjects.Money;
import com.claircore.billing.domain.model.valueobjects.SubscriptionStatus;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class Subscription extends AbstractAggregateRoot<Subscription> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    private UserId userId;

    @Embedded
    private Money amount;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    private String stripePaymentIntentId;

    @Embedded
    private SubscriptionAudit auditFields = new SubscriptionAudit();

    protected Subscription() {}

    public Subscription(UserId userId, Money amount, String stripePaymentIntentId) {
        this.userId = userId;
        this.amount = amount;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.status = SubscriptionStatus.PENDING;
    }

    public void markAsActive() {
        if (this.status == SubscriptionStatus.PENDING) {
            this.status = SubscriptionStatus.ACTIVE;
            this.registerEvent(new SubscriptionPaidEvent(this, this.stripePaymentIntentId, this.userId));
        } else {
            throw new IllegalStateException("Subscription can only be activated from PENDING status");
        }
    }

    // Getters
    public UUID getId() { return id; }
    public UserId getUserId() { return userId; }
    public Money getAmount() { return amount; }
    public SubscriptionStatus getStatus() { return status; }
    public String getStripePaymentIntentId() { return stripePaymentIntentId; }

    @Embeddable
    public static class SubscriptionAudit extends AuditableModel {}
}
