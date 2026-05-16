package com.claircore.device.application.internal.outboundservices.acl;

import com.claircore.billing.interfaces.acl.BillingContextFacade;
import com.claircore.billing.domain.model.valueobjects.PlanType;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ExternalBillingService {

    private final BillingContextFacade billingContextFacade;

    public ExternalBillingService(BillingContextFacade billingContextFacade) {
        this.billingContextFacade = billingContextFacade;
    }

    public PlanType getUserPlanType(UUID userId) {
        return billingContextFacade.getUserPlanType(userId);
    }

    public boolean canCreateOrganization(UUID userId) {
        PlanType plan = getUserPlanType(userId);
        return plan == PlanType.FREEMIUM || plan == PlanType.PREMIUM;
    }

    public int getMaxOrganizations(UUID userId) {
        return switch (getUserPlanType(userId)) {
            case PREMIUM -> 3;
            case FREEMIUM -> 1;
            default -> 1;
        };
    }

    public int getMaxSpaces(UUID userId) {
        return switch (getUserPlanType(userId)) {
            case PREMIUM -> 5;
            case FREEMIUM -> 1;
            default -> 1;
        };
    }

    public int getMaxDevices(UUID userId) {
        return switch (getUserPlanType(userId)) {
            case PREMIUM -> 10;
            case FREEMIUM -> 1;
            default -> 1;
        };
    }
}