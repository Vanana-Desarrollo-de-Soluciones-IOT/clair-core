package com.claircore.device.application.internal.commandservices;

import com.claircore.device.domain.model.commands.UpdateDevicePresenceStatusCommand;
import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.ClaimToken;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.DeviceType;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevicePresenceCommandServiceImplTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceAssignmentRepository deviceAssignmentRepository;

    @Test
    void shouldUpdatePresenceWhenDeviceIsFoundByDeviceId() {
        Device device = device();
        DeviceAssignment assignment = new DeviceAssignment(device.getId(), ClaimToken.generate());
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(device.getId())).thenReturn(Optional.of(assignment));
        when(deviceAssignmentRepository.save(any(DeviceAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceAssignment result = new DevicePresenceCommandServiceImpl(deviceRepository, deviceAssignmentRepository)
                .handle(new UpdateDevicePresenceStatusCommand(device.getId(), null, DeviceStatus.ONLINE, Instant.now()));

        assertEquals(DeviceStatus.ONLINE, result.getStatus());
        verify(deviceAssignmentRepository).save(assignment);
    }

    @Test
    void shouldUpdatePresenceWhenDeviceIsFoundByHardwareId() {
        Device device = device();
        DeviceAssignment assignment = new DeviceAssignment(device.getId(), ClaimToken.generate());
        when(deviceRepository.findByHardwareId(device.getHardwareId().value())).thenReturn(Optional.of(device));
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(device.getId())).thenReturn(Optional.of(assignment));
        when(deviceAssignmentRepository.save(any(DeviceAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceAssignment result = new DevicePresenceCommandServiceImpl(deviceRepository, deviceAssignmentRepository)
                .handle(new UpdateDevicePresenceStatusCommand(null, device.getHardwareId(), DeviceStatus.STANDBY, Instant.now()));

        assertEquals(DeviceStatus.STANDBY, result.getStatus());
    }

    @Test
    void shouldThrowExceptionWhenDeviceIdAndHardwareIdDoNotMatch() {
        Device device = device();
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new DevicePresenceCommandServiceImpl(deviceRepository, deviceAssignmentRepository)
                        .handle(new UpdateDevicePresenceStatusCommand(device.getId(), new HardwareId("HW-0001"), DeviceStatus.ONLINE, Instant.now()))
        );

        assertEquals("Device ID and hardware ID do not match", exception.getMessage());
        verify(deviceAssignmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenAssignmentDoesNotExist() {
        Device device = device();
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(device.getId())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new DevicePresenceCommandServiceImpl(deviceRepository, deviceAssignmentRepository)
                        .handle(new UpdateDevicePresenceStatusCommand(device.getId(), null, DeviceStatus.ERROR, Instant.now()))
        );

        assertEquals("Device assignment not found", exception.getMessage());
    }

    private Device device() {
        Device device = new Device(
                "SN-0100",
                "Sensor 0100",
                new HardwareId("CLAIR-0KBG"),
                ApiKey.generate(),
                new DeviceType("air-quality-v1")
        );
        org.springframework.test.util.ReflectionTestUtils.setField(device, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655440700"));
        return device;
    }
}
