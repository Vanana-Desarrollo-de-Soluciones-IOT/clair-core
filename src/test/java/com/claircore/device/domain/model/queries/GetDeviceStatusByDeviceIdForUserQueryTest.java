package com.claircore.device.domain.model.queries;

import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GetDeviceStatusByDeviceIdForUserQueryTest {

    @Test
    void shouldCreateQueryWhenInputsAreValid() {
        UUID deviceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440410");
        UserId userId = new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440411"));

        GetDeviceStatusByDeviceIdForUserQuery query = new GetDeviceStatusByDeviceIdForUserQuery(deviceId, userId);

        assertEquals(deviceId, query.deviceId());
        assertEquals(userId, query.userId());
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetDeviceStatusByDeviceIdForUserQuery(UUID.randomUUID(), null)
        );

        assertEquals("userId must not be null", exception.getMessage());
    }
}
