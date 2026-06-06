package com.claircore.device.application.internal.outboundservices.acl;

import com.claircore.billing.interfaces.acl.BillingContextFacade;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalBillingServiceTest {

    @Test
    void shouldDelegateMaxSpacesLookupToBillingFacade() {
        BillingContextFacade facade = mock(BillingContextFacade.class);
        ExternalBillingService service = new ExternalBillingService(facade);
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440600");
        when(facade.getMaxSpaces(userId)).thenReturn(7);

        int result = service.getMaxSpaces(userId);

        assertEquals(7, result);
        verify(facade).getMaxSpaces(userId);
    }
}
