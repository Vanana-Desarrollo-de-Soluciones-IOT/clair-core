package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class ClaimDeviceCommandTest {

    @Test
    void shouldCreateCommandWhenRequiredFieldsAreValid() {
        UUID spaceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440300");
        UserId userId = new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440301"));

        ClaimDeviceCommand command = new ClaimDeviceCommand("AB45-F3B1", spaceId, userId);

        assertEquals("AB45-F3B1", command.claimToken());
        assertEquals(spaceId, command.spaceId());
        assertEquals(userId, command.userId());
    }

    @Test
    void shouldThrowExceptionWhenClaimTokenIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new ClaimDeviceCommand("", UUID.randomUUID(), new UserId(UUID.randomUUID()))
        );

        assertEquals("Claim token must not be null or blank", exception.getMessage());
    }
}
