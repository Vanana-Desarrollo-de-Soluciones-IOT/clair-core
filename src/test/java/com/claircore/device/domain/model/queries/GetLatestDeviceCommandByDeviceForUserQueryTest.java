package com.claircore.device.domain.model.queries;

import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GetLatestDeviceCommandByDeviceForUserQueryTest {

    @Test
    void shouldCreateQueryWhenInputsAreValid() {
        UUID deviceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440430");
        UserId userId = new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440431"));

        GetLatestDeviceCommandByDeviceForUserQuery query = new GetLatestDeviceCommandByDeviceForUserQuery(deviceId, userId);

        assertEquals(deviceId, query.deviceId());
        assertEquals(userId, query.userId());
    }

    @Test
    void shouldThrowExceptionWhenDeviceIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetLatestDeviceCommandByDeviceForUserQuery(null, new UserId(UUID.randomUUID()))
        );

        assertEquals("deviceId must not be null", exception.getMessage());
    }
}
