package com.claircore.device.domain.model.queries;

import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GetDeviceCommandByIdForUserQueryTest {

    @Test
    void shouldCreateQueryWhenInputsAreValid() {
        UUID deviceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440420");
        UUID commandId = UUID.fromString("550e8400-e29b-41d4-a716-446655440421");
        UserId userId = new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440422"));

        GetDeviceCommandByIdForUserQuery query = new GetDeviceCommandByIdForUserQuery(deviceId, commandId, userId);

        assertEquals(deviceId, query.deviceId());
        assertEquals(commandId, query.commandId());
        assertEquals(userId, query.userId());
    }

    @Test
    void shouldThrowExceptionWhenCommandIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetDeviceCommandByIdForUserQuery(UUID.randomUUID(), null, new UserId(UUID.randomUUID()))
        );

        assertEquals("commandId must not be null", exception.getMessage());
    }
}
