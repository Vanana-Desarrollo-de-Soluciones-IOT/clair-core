package com.claircore.billing.application.acl;

import com.claircore.billing.domain.model.aggregates.UserPlan;
import com.claircore.billing.domain.model.valueobjects.PlanType;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.interfaces.acl.BillingContextFacade;
import com.claircore.billing.domain.repositories.UserPlanRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class BillingContextFacadeImpl implements BillingContextFacade {

    private final UserPlanRepository userPlanRepository;

    public BillingContextFacadeImpl(UserPlanRepository userPlanRepository) {
        this.userPlanRepository = userPlanRepository;
    }

    private PlanType resolveUserPlanType(UUID userId) {
        return userPlanRepository.findByUserId(new UserId(userId))
                .map(UserPlan::getPlanType)
                .orElse(PlanType.FREEMIUM);
    }

    /**
     * Effective plan after honouring premium expiry. An expired premium plan is
     * treated as freemium for entitlement decisions (paid features must lock back).
     */
    private PlanType resolveEffectivePlanType(UUID userId) {
        return userPlanRepository.findByUserId(new UserId(userId))
                .map(plan -> plan.isPremiumExpired() ? PlanType.FREEMIUM : plan.getPlanType())
                .orElse(PlanType.FREEMIUM);
    }

    @Override
    public int getMaxOrganizations(UUID userId) {
        return resolveEffectivePlanType(userId).maxOrganizations();
    }

    @Override
    public int getMaxSpaces(UUID userId) {
        return resolveEffectivePlanType(userId).maxSpaces();
    }

    @Override
    public int getMaxDevices(UUID userId) {
        return resolveEffectivePlanType(userId).maxDevices();
    }

    @Override
    public boolean canAccessMonthlyReports(UUID userId) {
        return resolveEffectivePlanType(userId).monthlyReports();
    }
}
