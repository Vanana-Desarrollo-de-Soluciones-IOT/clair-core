package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class CreateSpaceCommandTest {

    @Test
    void shouldCreateCommandWhenValuesAreValid() {
        UUID organizationId = UUID.fromString("550e8400-e29b-41d4-a716-446655447010");
        UserId ownerUserId = new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655447011"));

        CreateSpaceCommand command = new CreateSpaceCommand("Kitchen", organizationId, ownerUserId);

        assertEquals("Kitchen", command.name());
        assertEquals(organizationId, command.organizationId());
        assertEquals(ownerUserId, command.ownerUserId());
    }

    @Test
    void shouldThrowExceptionWhenOrganizationIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new CreateSpaceCommand("Kitchen", null, new UserId(UUID.randomUUID()))
        );

        assertEquals("Organization ID must not be null", exception.getMessage());
    }
}
