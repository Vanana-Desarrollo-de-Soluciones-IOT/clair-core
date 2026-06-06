package com.claircore.device.domain.model.queries;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GetDevicesBySpaceQueryTest {

    @Test
    void shouldCreateQueryWhenPaginationParametersAreOmitted() {
        UUID spaceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");

        GetDevicesBySpaceQuery query = new GetDevicesBySpaceQuery(spaceId, null, null);

        assertEquals(spaceId, query.spaceId());
        assertEquals(null, query.page());
        assertEquals(null, query.size());
    }

    @Test
    void shouldThrowExceptionWhenSpaceIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetDevicesBySpaceQuery(null, 0, 20)
        );

        assertEquals("Space ID must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenPageIsNegative() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetDevicesBySpaceQuery(UUID.randomUUID(), -1, 20)
        );

        assertEquals("Page must be non-negative", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenSizeIsLessThanOne() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetDevicesBySpaceQuery(UUID.randomUUID(), 0, 0)
        );

        assertEquals("Size must be at least 1", exception.getMessage());
    }
}
