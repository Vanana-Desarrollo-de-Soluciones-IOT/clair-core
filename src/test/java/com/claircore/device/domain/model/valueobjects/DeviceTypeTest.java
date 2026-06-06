package com.claircore.device.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class DeviceTypeTest {

    @Test
    void shouldCreateDeviceTypeWhenValueIsValid() {
        DeviceType deviceType = new DeviceType("air-quality-v1");

        assertEquals("air-quality-v1", deviceType.value());
    }

    @Test
    void shouldThrowExceptionWhenValueIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new DeviceType(" ")
        );

        assertEquals("Device type must not be null or blank", exception.getMessage());
    }
}
