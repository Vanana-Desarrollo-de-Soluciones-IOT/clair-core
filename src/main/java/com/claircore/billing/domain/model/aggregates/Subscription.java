package com.claircore.billing.domain.model.aggregates;

import com.claircore.billing.domain.model.events.SubscriptionPaidEvent;
import com.claircore.billing.domain.model.valueobjects.Money;
import com.claircore.billing.domain.model.valueobjects.SubscriptionStatus;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.util.UUID;

@Entity
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
    @AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false))
    })
    private AuditFields auditFields = new AuditFields();

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
    @EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
    public static class AuditFields extends AuditableModel {}
}
