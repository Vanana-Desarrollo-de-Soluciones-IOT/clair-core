package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.domain.model.commands.UpdateDeviceNameCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceSerialNumberCommand;
import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.OrganizationRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.SpaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceCommandServiceImplTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ExternalBillingService externalBillingService;

    @InjectMocks
    private DeviceCommandServiceImpl service;

    @Test
    void updateDeviceNameSucceeds() {
        UUID deviceId = UUID.randomUUID();
        Device device = deviceWithId(deviceId, "ABC-1");
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        service.handle(new UpdateDeviceNameCommand(deviceId, "Thermostat"));

        verify(deviceRepository).save(device);
    }

    @Test
    void updateDeviceSerialNumberSucceedsWhenSerialBelongsToSameDevice() {
        UUID deviceId = UUID.randomUUID();
        Device device = deviceWithId(deviceId, "ABC-1");
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(deviceRepository.findBySerialNumber("ABC-1")).thenReturn(Optional.of(device));

        service.handle(new UpdateDeviceSerialNumberCommand(deviceId, "ABC-1"));

        verify(deviceRepository).save(device);
    }

    @Test
    void updateDeviceSerialNumberFailsWhenSerialBelongsToAnotherDevice() {
        UUID deviceId = UUID.randomUUID();
        Device device = deviceWithId(deviceId, "ABC-1");
        Device otherDevice = deviceWithId(UUID.randomUUID(), "XYZ-1");
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(deviceRepository.findBySerialNumber("XYZ-1")).thenReturn(Optional.of(otherDevice));

        assertThrows(IllegalArgumentException.class, () -> service.handle(new UpdateDeviceSerialNumberCommand(deviceId, "XYZ-1")));

        verify(deviceRepository, never()).save(device);
    }

    private Device deviceWithId(UUID deviceId, String serialNumber) {
        Device device = new Device(serialNumber, "Sensor", UUID.randomUUID());
        ReflectionTestUtils.setField(device, "id", deviceId);
        return device;
    }
}
