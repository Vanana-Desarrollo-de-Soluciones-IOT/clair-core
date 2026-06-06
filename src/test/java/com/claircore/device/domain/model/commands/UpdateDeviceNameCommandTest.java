package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class UpdateDeviceNameCommandTest {

    @Test
    void shouldCreateCommandWhenValuesAreValid() {
        UUID deviceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440320");
        UserId userId = new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440321"));

        UpdateDeviceNameCommand command = new UpdateDeviceNameCommand(deviceId, "Kitchen sensor", userId);

        assertEquals(deviceId, command.deviceId());
        assertEquals("Kitchen sensor", command.name());
        assertEquals(userId, command.userId());
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new UpdateDeviceNameCommand(UUID.randomUUID(), " ", new UserId(UUID.randomUUID()))
        );

        assertEquals("Device name must not be null or blank", exception.getMessage());
    }
}
