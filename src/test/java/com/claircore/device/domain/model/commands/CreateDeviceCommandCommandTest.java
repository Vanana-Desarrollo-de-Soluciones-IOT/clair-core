package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.DeviceCommandType;
import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class CreateDeviceCommandCommandTest {

    @Test
    void shouldCreateCommandWhenAllRequiredFieldsArePresent() {
        UUID deviceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UserId userId = new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        CreateDeviceCommandCommand command = new CreateDeviceCommandCommand(
                deviceId,
                DeviceCommandType.RESTART,
                "{}",
                userId
        );

        assertEquals(deviceId, command.deviceId());
        assertEquals(DeviceCommandType.RESTART, command.type());
        assertEquals("{}", command.payload());
        assertEquals(userId, command.userId());
    }

    @Test
    void shouldThrowExceptionWhenDeviceIdIsNull() {
        UserId userId = new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new CreateDeviceCommandCommand(null, DeviceCommandType.WAKE, "{}", userId)
        );

        assertEquals("Device ID must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenCommandTypeIsNull() {
        UserId userId = new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new CreateDeviceCommandCommand(UUID.randomUUID(), null, "{}", userId)
        );

        assertEquals("Device command type must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new CreateDeviceCommandCommand(UUID.randomUUID(), DeviceCommandType.STANDBY, "{}", null)
        );

        assertEquals("User ID must not be null", exception.getMessage());
    }
}
