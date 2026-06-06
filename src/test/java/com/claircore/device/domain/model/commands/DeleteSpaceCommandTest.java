package com.claircore.device.domain.model.commands;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class DeleteSpaceCommandTest {

    @Test
    void shouldCreateCommandWhenSpaceIdIsValid() {
        UUID spaceId = UUID.fromString("550e8400-e29b-41d4-a716-446655447020");

        DeleteSpaceCommand command = new DeleteSpaceCommand(spaceId);

        assertEquals(spaceId, command.spaceId());
    }

    @Test
    void shouldThrowExceptionWhenSpaceIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new DeleteSpaceCommand(null)
        );

        assertEquals("Space ID must not be null", exception.getMessage());
    }
}
