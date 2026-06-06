package com.claircore.device.domain.model.queries;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GetDeviceBySerialNumberQueryTest {

    @Test
    void shouldCreateQueryWhenSerialNumberIsValid() {
        GetDeviceBySerialNumberQuery query = new GetDeviceBySerialNumberQuery("SN-0001");

        assertEquals("SN-0001", query.serialNumber());
    }

    @Test
    void shouldThrowExceptionWhenSerialNumberIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetDeviceBySerialNumberQuery(" ")
        );

        assertEquals("Serial number must not be null or blank", exception.getMessage());
    }
}
