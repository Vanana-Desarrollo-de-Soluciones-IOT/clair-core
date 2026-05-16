package com.claircore.billing.infrastructure.persistence.jpa.repositories;

import com.claircore.billing.domain.model.aggregates.PaymentRecord;
import com.claircore.billing.domain.model.valueobjects.UserId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {
    Optional<PaymentRecord> findByStripePaymentIntentId(String stripePaymentIntentId);
    List<PaymentRecord> findAllByUserId(UserId userId);
}
