package com.claircore.device.domain.model.queries;

import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GetOrganizationsByOwnerQueryTest {

    @Test
    void shouldCreateQueryWhenOwnerUserIdIsValid() {
        UserId ownerUserId = new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655447041"));

        GetOrganizationsByOwnerQuery query = new GetOrganizationsByOwnerQuery(ownerUserId);

        assertEquals(ownerUserId, query.ownerUserId());
    }

    @Test
    void shouldThrowExceptionWhenOwnerUserIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetOrganizationsByOwnerQuery(null)
        );

        assertEquals("Owner user ID must not be null", exception.getMessage());
    }
}
