package com.claircore.device.application.internal.outboundservices.acl;

import com.claircore.billing.interfaces.acl.BillingContextFacade;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ExternalBillingService {

    private final BillingContextFacade billingContextFacade;

    public ExternalBillingService(BillingContextFacade billingContextFacade) {
        this.billingContextFacade = billingContextFacade;
    }

    public int getMaxOrganizations(UUID userId) {
        return billingContextFacade.getMaxOrganizations(userId);
    }

    public int getMaxSpaces(UUID userId) {
        return billingContextFacade.getMaxSpaces(userId);
    }

    public int getMaxDevices(UUID userId) {
        return billingContextFacade.getMaxDevices(userId);
    }
}
