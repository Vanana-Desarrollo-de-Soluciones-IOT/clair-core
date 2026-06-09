package com.claircore.billing.domain.model.aggregates;

import com.claircore.billing.domain.model.events.SubscriptionPaidEvent;
import com.claircore.billing.domain.model.valueobjects.Money;
import com.claircore.billing.domain.model.valueobjects.PaymentStatus;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class PaymentRecord extends AbstractAggregateRoot<PaymentRecord> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    private UserId userId;

    @Embedded
    private Money amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String stripePaymentIntentId;

    @Embedded
    private PaymentRecordAudit auditFields = new PaymentRecordAudit();

    protected PaymentRecord() {}

    public PaymentRecord(UserId userId, Money amount, String stripePaymentIntentId) {
        this.userId = userId;
        this.amount = amount;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.status = PaymentStatus.PENDING;
    }

    public void markAsCompleted() {
        if (this.status == PaymentStatus.PENDING) {
            this.status = PaymentStatus.COMPLETED;
            this.registerEvent(new SubscriptionPaidEvent(this, this.stripePaymentIntentId, this.userId));
        } else {
            throw new IllegalStateException("PaymentRecord can only be completed from PENDING status");
        }
    }

    // Getters
    public UUID getId() { return id; }
    public UserId getUserId() { return userId; }
    public Money getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public String getStripePaymentIntentId() { return stripePaymentIntentId; }

    @Embeddable
    public static class PaymentRecordAudit extends AuditableModel {}
}
