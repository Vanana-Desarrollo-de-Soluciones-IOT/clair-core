package com.claircore.device.domain.model.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class SeedDevicesCommandTest {

    @Test
    void shouldCreateCommandWhenCountIsPositive() {
        SeedDevicesCommand command = new SeedDevicesCommand(5);

        assertEquals(5, command.count());
    }

    @Test
    void shouldThrowExceptionWhenCountIsNotPositive() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new SeedDevicesCommand(0)
        );

        assertEquals("Count must be positive", exception.getMessage());
    }
}
