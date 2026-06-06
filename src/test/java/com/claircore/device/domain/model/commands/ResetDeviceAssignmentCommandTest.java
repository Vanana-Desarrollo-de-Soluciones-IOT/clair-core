package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class ResetDeviceAssignmentCommandTest {

    @Test
    void shouldCreateCommandWhenValuesAreValid() {
        UUID deviceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440310");
        UserId userId = new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440311"));

        ResetDeviceAssignmentCommand command = new ResetDeviceAssignmentCommand(deviceId, userId);

        assertEquals(deviceId, command.deviceId());
        assertEquals(userId, command.userId());
    }

    @Test
    void shouldThrowExceptionWhenDeviceIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new ResetDeviceAssignmentCommand(null, new UserId(UUID.randomUUID()))
        );

        assertEquals("Device ID must not be null", exception.getMessage());
    }
}
