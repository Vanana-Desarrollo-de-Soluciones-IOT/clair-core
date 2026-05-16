package com.claircore.billing.infrastructure.persistence.jpa.repositories;

import com.claircore.billing.domain.model.aggregates.UserPlan;
import com.claircore.billing.domain.model.valueobjects.UserId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserPlanRepository extends JpaRepository<UserPlan, UUID> {
    Optional<UserPlan> findByUserId(UserId userId);
}
