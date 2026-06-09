package com.claircore.device.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class HardwareIdTest {

    @Test
    void shouldCreateHardwareIdWhenUsingCurrentFactoryFormat() {
        HardwareId hardwareId = new HardwareId("CLAIR-0KBG");

        assertEquals("CLAIR-0KBG", hardwareId.value());
    }

    @Test
    void shouldCreateHardwareIdWhenUsingLegacyFormat() {
        HardwareId hardwareId = new HardwareId("HW-0001");

        assertEquals("HW-0001", hardwareId.value());
    }

    @Test
    void shouldThrowExceptionWhenHardwareIdIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new HardwareId(" ")
        );

        assertEquals("Hardware ID must not be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenHardwareIdHasInvalidFormat() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new HardwareId("bad-id")
        );

        assertEquals("Hardware ID must match CLAIR-0KBG or HW-0001", exception.getMessage());
    }
}
