package com.claircore.device.application.internal.queryservices;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.domain.model.queries.GetDeviceCommandByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetLatestDeviceCommandByDeviceForUserQuery;
import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceCommandRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceCommandQueryServiceImplTest {

    @Mock
    private DeviceCommandRepository deviceCommandRepository;

    @Mock
    private DeviceAssignmentRepository deviceAssignmentRepository;

    @Test
    void shouldReturnCommandWhenDeviceAndUserMatch() {
        DeviceAssignment assignment = ownedAssignment();
        DeviceCommand command = new DeviceCommand(assignment.getDevice(), DeviceCommandType.WAKE, "{}");
        org.springframework.test.util.ReflectionTestUtils.setField(command, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655441100"));
        when(deviceAssignmentRepository.findByDeviceId(assignment.getDevice().getId())).thenReturn(Optional.of(assignment));
        when(deviceCommandRepository.findByDeviceIdAndCommandId(assignment.getDevice().getId(), command.getId())).thenReturn(Optional.of(command));

        Optional<DeviceCommand> result = new DeviceCommandQueryServiceImpl(deviceCommandRepository, deviceAssignmentRepository)
                .handle(new GetDeviceCommandByIdForUserQuery(assignment.getDevice().getId(), command.getId(), assignment.getOwnerUserId()));

        assertEquals(true, result.isPresent());
    }

    @Test
    void shouldReturnLatestCommandWhenDeviceAndUserMatch() {
        DeviceAssignment assignment = ownedAssignment();
        DeviceCommand command = new DeviceCommand(assignment.getDevice(), DeviceCommandType.RESTART, "{}");
        when(deviceAssignmentRepository.findByDeviceId(assignment.getDevice().getId())).thenReturn(Optional.of(assignment));
        when(deviceCommandRepository.findLatestByDeviceId(assignment.getDevice().getId())).thenReturn(Optional.of(command));

        Optional<DeviceCommand> result = new DeviceCommandQueryServiceImpl(deviceCommandRepository, deviceAssignmentRepository)
                .handle(new GetLatestDeviceCommandByDeviceForUserQuery(assignment.getDevice().getId(), assignment.getOwnerUserId()));

        assertEquals(true, result.isPresent());
    }

    @Test
    void shouldThrowAccessDeniedWhenDeviceDoesNotBelongToUser() {
        DeviceAssignment assignment = ownedAssignment();
        when(deviceAssignmentRepository.findByDeviceId(assignment.getDevice().getId())).thenReturn(Optional.of(assignment));

        assertThrowsExactly(
                org.springframework.security.access.AccessDeniedException.class,
                () -> new DeviceCommandQueryServiceImpl(deviceCommandRepository, deviceAssignmentRepository)
                        .handle(new GetLatestDeviceCommandByDeviceForUserQuery(assignment.getDevice().getId(), new UserId(UUID.randomUUID())))
        );
    }

    private DeviceAssignment ownedAssignment() {
        Device device = new Device("SN-0500", "Sensor 0500", new HardwareId("CLAIR-0KBG"), ApiKey.generate(), new DeviceType("air-quality-v1"));
        org.springframework.test.util.ReflectionTestUtils.setField(device, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655441101"));
        DeviceAssignment assignment = new DeviceAssignment(device, ClaimToken.generate());
        assignment.claimToSpace(UUID.randomUUID(), new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655441102")));
        return assignment;
    }
}
