package com.claircore.device.domain.model.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class PairDeviceCommandTest {

    @Test
    void shouldCreateCommandWhenHardwareIdIsValid() {
        PairDeviceCommand command = new PairDeviceCommand("HW-0001");

        assertEquals("HW-0001", command.hardwareId());
    }

    @Test
    void shouldThrowExceptionWhenHardwareIdIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new PairDeviceCommand(" ")
        );

        assertEquals("Hardware ID must not be null or blank", exception.getMessage());
    }
}
