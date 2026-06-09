package com.claircore.device.domain.model.queries;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GetDeviceByIdQueryTest {

    @Test
    void shouldCreateQueryWhenDeviceIdIsValid() {
        UUID deviceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440400");

        GetDeviceByIdQuery query = new GetDeviceByIdQuery(deviceId);

        assertEquals(deviceId, query.deviceId());
    }

    @Test
    void shouldThrowExceptionWhenDeviceIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetDeviceByIdQuery(null)
        );

        assertEquals("Device ID must not be null", exception.getMessage());
    }
}
