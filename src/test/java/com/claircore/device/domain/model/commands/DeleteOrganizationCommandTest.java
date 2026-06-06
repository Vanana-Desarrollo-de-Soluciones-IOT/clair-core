package com.claircore.device.domain.model.commands;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class DeleteOrganizationCommandTest {

    @Test
    void shouldCreateCommandWhenOrganizationIdIsValid() {
        UUID organizationId = UUID.fromString("550e8400-e29b-41d4-a716-446655447001");

        DeleteOrganizationCommand command = new DeleteOrganizationCommand(organizationId);

        assertEquals(organizationId, command.organizationId());
    }

    @Test
    void shouldThrowExceptionWhenOrganizationIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new DeleteOrganizationCommand(null)
        );

        assertEquals("Organization ID must not be null", exception.getMessage());
    }
}
