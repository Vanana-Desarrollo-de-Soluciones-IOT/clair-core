package com.claircore.device.domain.model.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class DispatchPendingDeviceCommandsCommandTest {

    @Test
    void shouldCreateCommandWhenLimitIsNullOrPositive() {
        DispatchPendingDeviceCommandsCommand withNullLimit = new DispatchPendingDeviceCommandsCommand(null);
        DispatchPendingDeviceCommandsCommand withLimit = new DispatchPendingDeviceCommandsCommand(25);

        assertEquals(null, withNullLimit.limit());
        assertEquals(25, withLimit.limit());
    }

    @Test
    void shouldThrowExceptionWhenLimitIsNotPositive() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new DispatchPendingDeviceCommandsCommand(0)
        );

        assertEquals("Limit must be positive", exception.getMessage());
    }
}
