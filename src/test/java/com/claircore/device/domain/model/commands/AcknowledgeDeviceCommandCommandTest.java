package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class AcknowledgeDeviceCommandCommandTest {

    @Test
    void shouldCreateCommandWhenAcknowledgementStatusIsValid() {
        UUID deviceId = UUID.fromString("550e8400-e29b-41d4-a716-446655447030");
        UUID commandId = UUID.fromString("550e8400-e29b-41d4-a716-446655447031");

        AcknowledgeDeviceCommandCommand command = new AcknowledgeDeviceCommandCommand(
                deviceId,
                commandId,
                DeviceCommandStatus.EXECUTED,
                null
        );

        assertEquals(deviceId, command.deviceId());
        assertEquals(commandId, command.commandId());
        assertEquals(DeviceCommandStatus.EXECUTED, command.status());
    }

    @Test
    void shouldThrowExceptionWhenStatusIsNotExecutableOrFailed() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new AcknowledgeDeviceCommandCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        DeviceCommandStatus.SENT,
                        null
                )
        );

        assertEquals("ACK status must be EXECUTED or FAILED", exception.getMessage());
    }
}
