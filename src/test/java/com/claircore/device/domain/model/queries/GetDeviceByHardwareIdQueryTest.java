package com.claircore.device.domain.model.queries;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GetDeviceByHardwareIdQueryTest {

    @Test
    void shouldCreateQueryWhenHardwareIdIsValid() {
        GetDeviceByHardwareIdQuery query = new GetDeviceByHardwareIdQuery("CLAIR-0KBG");

        assertEquals("CLAIR-0KBG", query.hardwareId());
    }

    @Test
    void shouldThrowExceptionWhenHardwareIdIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetDeviceByHardwareIdQuery(" ")
        );

        assertEquals("Hardware ID must not be null or blank", exception.getMessage());
    }
}
