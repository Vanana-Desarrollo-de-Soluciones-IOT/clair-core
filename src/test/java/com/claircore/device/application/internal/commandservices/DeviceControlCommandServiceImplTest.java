package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.DeviceCommandsPendingPublisher;
import com.claircore.device.domain.model.commands.AcknowledgeDeviceCommandCommand;
import com.claircore.device.domain.model.commands.CreateDeviceCommandCommand;
import com.claircore.device.domain.model.commands.DispatchPendingDeviceCommandsCommand;
import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceCommandRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceControlCommandServiceImplTest {

    @Mock
    private DeviceAssignmentRepository deviceAssignmentRepository;

    @Mock
    private DeviceCommandRepository deviceCommandRepository;

    @Mock
    private DeviceCommandsPendingPublisher deviceCommandsPendingPublisher;

    @Test
    void shouldCreateDeviceCommandWhenDeviceBelongsToUser() {
        DeviceAssignment assignment = ownedAssignment();
        when(deviceAssignmentRepository.findByDeviceId(assignment.getDevice().getId())).thenReturn(Optional.of(assignment));
        when(deviceCommandRepository.save(any(DeviceCommand.class))).thenAnswer(invocation -> {
            DeviceCommand saved = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655443500"));
            return saved;
        });

        DeviceCommand result = new DeviceControlCommandServiceImpl(
                deviceAssignmentRepository,
                deviceCommandRepository,
                deviceCommandsPendingPublisher
        ).handle(new CreateDeviceCommandCommand(
                assignment.getDevice().getId(),
                DeviceCommandType.WAKE,
                "{}",
                assignment.getOwnerUserId()
        ));

        assertEquals(DeviceCommandStatus.PENDING, result.getStatus());
        verify(deviceCommandRepository).save(any(DeviceCommand.class));
    }

    @Test
    void shouldThrowAccessDeniedWhenDeviceDoesNotBelongToUser() {
        DeviceAssignment assignment = ownedAssignment();
        when(deviceAssignmentRepository.findByDeviceId(assignment.getDevice().getId())).thenReturn(Optional.of(assignment));

        assertThrowsExactly(
                org.springframework.security.access.AccessDeniedException.class,
                () -> new DeviceControlCommandServiceImpl(deviceAssignmentRepository, deviceCommandRepository, deviceCommandsPendingPublisher)
                        .handle(new CreateDeviceCommandCommand(
                                assignment.getDevice().getId(),
                                DeviceCommandType.WAKE,
                                "{}",
                                new UserId(UUID.randomUUID())
                        ))
        );

        verify(deviceCommandRepository, never()).save(any());
    }

    @Test
    void shouldMarkCommandsAsSentWhenDispatchingPendingCommands() {
        DeviceCommand first = new DeviceCommand(ownedAssignment().getDevice(), DeviceCommandType.STANDBY, "{}");
        DeviceCommand second = new DeviceCommand(ownedAssignment().getDevice(), DeviceCommandType.RESTART, "{}");
        when(deviceCommandRepository.findByStatusForDispatch(DeviceCommandStatus.PENDING, org.springframework.data.domain.PageRequest.of(0, 2)))
                .thenReturn(List.of(first, second));
        when(deviceCommandRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<DeviceCommand> result = new DeviceControlCommandServiceImpl(deviceAssignmentRepository, deviceCommandRepository, deviceCommandsPendingPublisher)
                .handle(new DispatchPendingDeviceCommandsCommand(2));

        assertEquals(DeviceCommandStatus.SENT, result.get(0).getStatus());
        assertEquals(DeviceCommandStatus.SENT, result.get(1).getStatus());
        verify(deviceCommandRepository).saveAll(List.of(first, second));
    }

    @Test
    void shouldMarkCommandAsExecutedWhenAcknowledgedSuccessfully() {
        DeviceAssignment assignment = ownedAssignment();
        DeviceCommand command = new DeviceCommand(assignment.getDevice(), DeviceCommandType.WAKE, "{}");
        org.springframework.test.util.ReflectionTestUtils.setField(command, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655440900"));
        when(deviceCommandRepository.findByDeviceIdAndCommandId(assignment.getDevice().getId(), command.getId())).thenReturn(Optional.of(command));
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(assignment.getDevice().getId())).thenReturn(Optional.of(assignment));
        when(deviceCommandRepository.save(any(DeviceCommand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceCommand result = new DeviceControlCommandServiceImpl(deviceAssignmentRepository, deviceCommandRepository, deviceCommandsPendingPublisher)
                .handle(new AcknowledgeDeviceCommandCommand(assignment.getDevice().getId(), command.getId(), DeviceCommandStatus.EXECUTED, null));

        assertEquals(DeviceCommandStatus.EXECUTED, result.getStatus());
        verify(deviceAssignmentRepository).save(assignment);
    }

    private DeviceAssignment ownedAssignment() {
        Device device = new Device(
                "SN-0300",
                "Sensor 0300",
                new HardwareId("CLAIR-0KBG"),
                ApiKey.generate(),
                new DeviceType("air-quality-v1")
        );
        org.springframework.test.util.ReflectionTestUtils.setField(device, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655440901"));
        DeviceAssignment assignment = new DeviceAssignment(device, ClaimToken.generate());
        assignment.claimToSpace(UUID.fromString("550e8400-e29b-41d4-a716-446655440902"), new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440903")));
        return assignment;
    }
}
