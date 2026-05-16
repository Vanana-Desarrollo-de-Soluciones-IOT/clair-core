package com.claircore.billing.application.acl;

import com.claircore.billing.domain.model.aggregates.UserPlan;
import com.claircore.billing.domain.model.valueobjects.PlanType;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.interfaces.acl.BillingContextFacade;
import com.claircore.billing.infrastructure.persistence.jpa.repositories.UserPlanRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class BillingContextFacadeImpl implements BillingContextFacade {

    private final UserPlanRepository userPlanRepository;

    public BillingContextFacadeImpl(UserPlanRepository userPlanRepository) {
        this.userPlanRepository = userPlanRepository;
    }

    @Override
    public PlanType getUserPlanType(UUID userId) {
        return userPlanRepository.findByUserId(new UserId(userId))
                .map(UserPlan::getPlanType)
                .orElse(PlanType.VISITOR);
    }
}