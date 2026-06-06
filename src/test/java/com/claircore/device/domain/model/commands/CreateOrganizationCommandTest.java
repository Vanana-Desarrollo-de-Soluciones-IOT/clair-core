package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class CreateOrganizationCommandTest {

    @Test
    void shouldCreateCommandWhenValuesAreValid() {
        UserId ownerUserId = new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655447000"));

        CreateOrganizationCommand command = new CreateOrganizationCommand("Home", ownerUserId);

        assertEquals("Home", command.name());
        assertEquals(ownerUserId, command.ownerUserId());
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new CreateOrganizationCommand(" ", new UserId(UUID.randomUUID()))
        );

        assertEquals("Organization name must not be null or blank", exception.getMessage());
    }
}
