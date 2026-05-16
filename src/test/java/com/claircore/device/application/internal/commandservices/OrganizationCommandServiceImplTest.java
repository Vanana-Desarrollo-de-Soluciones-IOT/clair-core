package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.domain.model.commands.DeleteOrganizationCommand;
import com.claircore.device.domain.model.entities.Organization;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationCommandServiceImplTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private ExternalBillingService externalBillingService;

    @InjectMocks
    private OrganizationCommandServiceImpl service;

    @Test
    void deleteOrganizationFailsWhenOrganizationHasRegisteredDevices() {
        UUID organizationId = UUID.randomUUID();
        Organization organization = new Organization("Home", new UserId(UUID.randomUUID()));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(deviceRepository.existsByOrganizationId(organizationId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.handle(new DeleteOrganizationCommand(organizationId)));

        verify(organizationRepository, never()).delete(organization);
    }

    @Test
    void deleteOrganizationSucceedsWhenOrganizationHasNoRegisteredDevices() {
        UUID organizationId = UUID.randomUUID();
        Organization organization = new Organization("Home", new UserId(UUID.randomUUID()));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(deviceRepository.existsByOrganizationId(organizationId)).thenReturn(false);

        service.handle(new DeleteOrganizationCommand(organizationId));

        verify(organizationRepository).delete(organization);
    }
}
