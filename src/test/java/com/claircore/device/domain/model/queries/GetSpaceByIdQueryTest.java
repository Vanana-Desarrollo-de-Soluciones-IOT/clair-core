package com.claircore.device.domain.model.queries;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GetSpaceByIdQueryTest {

    @Test
    void shouldCreateQueryWhenSpaceIdIsValid() {
        UUID spaceId = UUID.fromString("550e8400-e29b-41d4-a716-446655447042");

        GetSpaceByIdQuery query = new GetSpaceByIdQuery(spaceId);

        assertEquals(spaceId, query.spaceId());
    }

    @Test
    void shouldThrowExceptionWhenSpaceIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetSpaceByIdQuery(null)
        );

        assertEquals("Space ID must not be null", exception.getMessage());
    }
}
