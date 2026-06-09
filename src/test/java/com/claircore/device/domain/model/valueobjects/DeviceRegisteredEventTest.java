package com.claircore.device.domain.model.valueobjects;

import com.claircore.device.domain.model.events.DeviceRegisteredEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class DeviceRegisteredEventTest {

    @Test
    void shouldCreateEventWhenRequiredDataIsPresent() {
        Instant occurredOn = Instant.parse("2026-06-05T12:00:00Z");

        DeviceRegisteredEvent event = new DeviceRegisteredEvent(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440200"),
                "CLAIR-0KBG",
                occurredOn
        );

        assertEquals("CLAIR-0KBG", event.hardwareId());
        assertEquals(occurredOn, event.occurredOn());
    }

    @Test
    void shouldThrowExceptionWhenDeviceIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new DeviceRegisteredEvent(null, "CLAIR-0KBG", Instant.now())
        );

        assertEquals("Device ID must not be null", exception.getMessage());
    }
}
