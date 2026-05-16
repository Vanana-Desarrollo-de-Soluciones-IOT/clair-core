package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.domain.model.commands.DeleteSpaceCommand;
import com.claircore.device.domain.model.commands.UpdateSpaceNameCommand;
import com.claircore.device.domain.model.entities.Space;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.OrganizationRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.SpaceRepository;
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
class SpaceCommandServiceImplTest {

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private ExternalBillingService externalBillingService;

    @InjectMocks
    private SpaceCommandServiceImpl service;

    @Test
    void deleteSpaceFailsWhenSpaceHasRegisteredDevices() {
        UUID spaceId = UUID.randomUUID();
        Space space = new Space("Living Room", UUID.randomUUID(), new UserId(UUID.randomUUID()));
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
        when(deviceRepository.existsBySpaceId(spaceId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.handle(new DeleteSpaceCommand(spaceId)));

        verify(spaceRepository, never()).delete(space);
    }

    @Test
    void deleteSpaceSucceedsWhenSpaceHasNoRegisteredDevices() {
        UUID spaceId = UUID.randomUUID();
        Space space = new Space("Living Room", UUID.randomUUID(), new UserId(UUID.randomUUID()));
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
        when(deviceRepository.existsBySpaceId(spaceId)).thenReturn(false);

        service.handle(new DeleteSpaceCommand(spaceId));

        verify(spaceRepository).delete(space);
    }

    @Test
    void updateSpaceNameSucceeds() {
        UUID spaceId = UUID.randomUUID();
        Space space = new Space("Living Room", UUID.randomUUID(), new UserId(UUID.randomUUID()));
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));

        service.handle(new UpdateSpaceNameCommand(spaceId, "Kitchen"));

        verify(spaceRepository).save(space);
    }
}
