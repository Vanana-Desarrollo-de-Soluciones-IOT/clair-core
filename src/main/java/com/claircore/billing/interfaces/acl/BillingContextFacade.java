package com.claircore.billing.interfaces.acl;

import com.claircore.billing.domain.model.valueobjects.PlanType;

import java.util.UUID;

public interface BillingContextFacade {

    PlanType getUserPlanType(UUID userId);
}