package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class UpdateDevicePresenceStatusCommandTest {

    @Test
    void shouldCreateCommandWhenDeviceIdAndStatusAreValid() {
        Instant occurredAt = Instant.parse("2026-06-05T12:30:00Z");

        UpdateDevicePresenceStatusCommand command = new UpdateDevicePresenceStatusCommand(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440330"),
                null,
                DeviceStatus.ONLINE,
                occurredAt
        );

        assertEquals(DeviceStatus.ONLINE, command.status());
        assertEquals(occurredAt, command.occurredAt());
    }

    @Test
    void shouldCreateCommandAndDefaultOccurredAtWhenNull() {
        UpdateDevicePresenceStatusCommand command = new UpdateDevicePresenceStatusCommand(
                null,
                new HardwareId("CLAIR-0KBG"),
                DeviceStatus.STANDBY,
                null
        );

        assertEquals(DeviceStatus.STANDBY, command.status());
        assertEquals(null, command.deviceId());
        assertEquals(new HardwareId("CLAIR-0KBG"), command.hardwareId());
        assertEquals(true, command.occurredAt() != null);
    }

    @Test
    void shouldThrowExceptionWhenStatusIsUnsupported() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new UpdateDevicePresenceStatusCommand(
                        UUID.randomUUID(),
                        null,
                        DeviceStatus.MAINTENANCE,
                        Instant.now()
                )
        );

        assertEquals("status must be ONLINE, OFFLINE, STANDBY, or ERROR", exception.getMessage());
    }
}
