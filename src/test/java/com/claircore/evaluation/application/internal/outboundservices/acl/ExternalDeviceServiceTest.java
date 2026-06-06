package com.claircore.evaluation.application.internal.outboundservices.acl;

import com.claircore.device.interfaces.acl.DeviceContextFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalDeviceServiceTest {

    @Mock
    private DeviceContextFacade deviceContextFacade;

    @InjectMocks
    private ExternalDeviceService externalDeviceService;

    @Test
    void shouldReturnDeviceIdWhenHardwareIdExists() {
        // Arrange
        String hardwareId = "HW-12345";
        UUID deviceId = UUID.randomUUID();
        when(deviceContextFacade.findDeviceIdByHardwareId(hardwareId)).thenReturn(Optional.of(deviceId));

        // Act
        Optional<UUID> result = externalDeviceService.findDeviceIdByHardwareId(hardwareId);

        // Assert
        assertThat(result).isPresent().contains(deviceId);
        verify(deviceContextFacade).findDeviceIdByHardwareId(hardwareId);
    }

    @Test
    void shouldReturnTrueWhenDeviceIsOwnedByUser() {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(deviceContextFacade.isDeviceOwnedByUser(deviceId, userId)).thenReturn(true);

        // Act
        boolean result = externalDeviceService.isDeviceOwnedByUser(deviceId, userId);

        // Assert
        assertThat(result).isTrue();
        verify(deviceContextFacade).isDeviceOwnedByUser(deviceId, userId);
    }
}
