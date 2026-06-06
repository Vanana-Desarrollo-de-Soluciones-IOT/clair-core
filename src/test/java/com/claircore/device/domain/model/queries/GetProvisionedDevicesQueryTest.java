package com.claircore.device.domain.model.queries;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GetProvisionedDevicesQueryTest {

    @Test
    void shouldCreateQueryWhenLimitIsPositive() {
        GetProvisionedDevicesQuery query = new GetProvisionedDevicesQuery(20);

        assertEquals(20, query.limit());
    }

    @Test
    void shouldThrowExceptionWhenLimitIsNotPositive() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetProvisionedDevicesQuery(0)
        );

        assertEquals("Limit must be a positive number", exception.getMessage());
    }
}
