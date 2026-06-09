package com.claircore.analytics.application.internal.outboundservices.acl;

import com.claircore.billing.interfaces.acl.BillingContextFacade;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Consumer-side ACL for resolving subscription entitlements from the Billing
 * bounded context. Mirrors the device context's ExternalBillingService.
 */
@Service("analyticsExternalBillingService")
public class ExternalBillingService {

    private final BillingContextFacade billingContextFacade;

    public ExternalBillingService(BillingContextFacade billingContextFacade) {
        this.billingContextFacade = billingContextFacade;
    }

    public boolean canAccessMonthlyReports(UUID userId) {
        return billingContextFacade.canAccessMonthlyReports(userId);
    }
}
