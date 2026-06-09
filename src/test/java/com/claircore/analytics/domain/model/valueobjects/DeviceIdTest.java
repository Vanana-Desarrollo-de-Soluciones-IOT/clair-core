package com.claircore.analytics.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DeviceIdTest {

    @Test
    void shouldCreateDeviceIdWhenValueIsValid() {
        var uuid = UUID.randomUUID();
        var deviceId = new DeviceId(uuid);
        assertNotNull(deviceId);
        assertEquals(uuid, deviceId.value());
    }

    @Test
    void shouldThrowExceptionWhenValueIsNull() {
        assertThrows(IllegalArgumentException.class, () -> 
            new DeviceId(null)
        );
    }
}
