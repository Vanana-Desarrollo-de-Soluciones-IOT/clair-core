package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.application.internal.outboundservices.webhooks.DeviceWebhookNotifier;
import com.claircore.device.domain.model.commands.ClaimDeviceCommand;
import com.claircore.device.domain.model.commands.PairDeviceCommand;
import com.claircore.device.domain.model.commands.ResetDeviceAssignmentCommand;
import com.claircore.device.domain.model.commands.SeedDevicesCommand;
import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.entities.Space;
import com.claircore.device.domain.model.valueobjects.ClaimToken;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.OrganizationRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.SpaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceCommandServiceImplTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceAssignmentRepository deviceAssignmentRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ExternalBillingService externalBillingService;

    @Mock
    private DeviceWebhookNotifier deviceWebhookNotifier;

    @InjectMocks
    private DeviceCommandServiceImpl service;

    @Test
    void seedDevicesCreatesNonExistingOnes() {
        when(deviceRepository.findBySerialNumber(any())).thenReturn(Optional.empty());
        when(deviceRepository.existsByHardwareId(any())).thenReturn(false);
        when(deviceRepository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));

        List<Device> result = service.handle(new SeedDevicesCommand(2));

        assertEquals(2, result.size());
        verify(deviceRepository, times(2)).save(any(Device.class));
    }

    @Test
    void pairDeviceFailsWhenHardwareNotFoundInFactoryInventory() {
        when(deviceRepository.findByHardwareId("HW-NEW-001")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.handle(new PairDeviceCommand("HW-NEW-001"))
        );

        verify(deviceAssignmentRepository, never()).save(any(DeviceAssignment.class));
    }

    @Test
    void pairDeviceCreatesAssignmentForFactoryDeviceWithoutAssignment() {
        Device existing = deviceWithId(UUID.randomUUID(), "SN-001", "HW-001");
        when(deviceRepository.findByHardwareId("HW-001")).thenReturn(Optional.of(existing));
        when(deviceAssignmentRepository.findByDeviceId(existing.getId())).thenReturn(Optional.empty());
        when(deviceAssignmentRepository.save(any(DeviceAssignment.class))).thenAnswer(i -> i.getArgument(0));

        DeviceAssignment result = service.handle(new PairDeviceCommand("HW-001"));

        assertEquals(existing, result.getDevice());
        verify(deviceRepository, never()).save(any(Device.class));
        verify(deviceAssignmentRepository).save(any(DeviceAssignment.class));
    }

    @Test
    void pairDeviceFailsWhenAlreadyPaired() {
        Device existing = deviceWithId(UUID.randomUUID(), "SN-001", "HW-001");
        DeviceAssignment assignment = new DeviceAssignment(existing, ClaimToken.generate());
        assignment.claimToSpace(UUID.randomUUID(), new UserId(UUID.randomUUID()));
        when(deviceRepository.findByHardwareId("HW-001")).thenReturn(Optional.of(existing));
        when(deviceAssignmentRepository.findByDeviceId(existing.getId())).thenReturn(Optional.of(assignment));

        assertThrows(IllegalStateException.class, () ->
            service.handle(new PairDeviceCommand("HW-001"))
        );
    }

    @Test
    void claimDeviceAssignsDeviceToUserOwnedSpace() {
        UUID userId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        Device device = deviceWithId(UUID.randomUUID(), "SN-002", "HW-002");
        DeviceAssignment assignment = new DeviceAssignment(device, ClaimToken.generate());
        Space space = spaceWithId(spaceId, userId);

        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
        when(deviceAssignmentRepository.findByClaimToken(assignment.getClaimToken().value())).thenReturn(Optional.of(assignment));
        when(deviceAssignmentRepository.save(any(DeviceAssignment.class))).thenAnswer(i -> i.getArgument(0));

        DeviceAssignment result = service.handle(new ClaimDeviceCommand(
            assignment.getClaimToken().value(),
            spaceId,
            new UserId(userId)
        ));

        assertEquals(spaceId, result.getSpaceId());
        assertEquals(new UserId(userId), result.getOwnerUserId());
        assertNotNull(result.getActivatedAt());
        verify(deviceWebhookNotifier).notifyDeviceChanged(result);
    }

    @Test
    void claimDeviceFailsWhenSpaceBelongsToAnotherUser() {
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        Space space = spaceWithId(spaceId, anotherUserId);

        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));

        assertThrows(AccessDeniedException.class, () ->
            service.handle(new ClaimDeviceCommand("claim-token", spaceId, new UserId(userId)))
        );
        verify(deviceAssignmentRepository, never()).findByClaimToken(any());
    }

    @Test
    void deleteDeviceDeletesAssignmentForOwnerOnly() {
        UUID userId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        Device device = deviceWithId(deviceId, "SN-003", "HW-003");
        DeviceAssignment assignment = new DeviceAssignment(device, ClaimToken.generate());
        assignment.claimToSpace(spaceId, new UserId(userId));

        when(deviceAssignmentRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(assignment));

        service.handle(new ResetDeviceAssignmentCommand(deviceId, new UserId(userId)));

        verify(deviceAssignmentRepository).delete(assignment);
        verify(deviceRepository, never()).delete(any(Device.class));
        verify(deviceWebhookNotifier).notifyDeviceDeleted(assignment);
    }

    private Device deviceWithId(UUID deviceId, String serialNumber, String hardwareId) {
        Device device = new Device(
            serialNumber,
            "Sensor",
            new com.claircore.device.domain.model.valueobjects.HardwareId(hardwareId),
            com.claircore.device.domain.model.valueobjects.ApiKey.generate(),
            new com.claircore.device.domain.model.valueobjects.DeviceType("air-quality-v1")
        );
        ReflectionTestUtils.setField(device, "id", deviceId);
        return device;
    }

    private Space spaceWithId(UUID spaceId, UUID ownerUserId) {
        Space space = new Space("Living Room", UUID.randomUUID(), new UserId(ownerUserId));
        ReflectionTestUtils.setField(space, "id", spaceId);
        return space;
    }
}
