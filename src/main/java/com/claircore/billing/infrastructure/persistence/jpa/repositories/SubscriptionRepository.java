package com.claircore.billing.infrastructure.persistence.jpa.repositories;

import com.claircore.billing.domain.model.aggregates.Subscription;
import com.claircore.billing.domain.model.valueobjects.SubscriptionStatus;
import com.claircore.billing.domain.model.valueobjects.UserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findAllByUserId(UserId userId);
    Optional<Subscription> findByStripePaymentIntentId(String stripePaymentIntentId);
    boolean existsByUserIdAndStatus(UserId userId, SubscriptionStatus status);
}
