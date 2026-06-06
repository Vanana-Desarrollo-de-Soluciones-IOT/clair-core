package com.claircore.device.domain.model.queries;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GetOrganizationByIdQueryTest {

    @Test
    void shouldCreateQueryWhenOrganizationIdIsValid() {
        UUID organizationId = UUID.fromString("550e8400-e29b-41d4-a716-446655447040");

        GetOrganizationByIdQuery query = new GetOrganizationByIdQuery(organizationId);

        assertEquals(organizationId, query.organizationId());
    }

    @Test
    void shouldThrowExceptionWhenOrganizationIdIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetOrganizationByIdQuery(null)
        );

        assertEquals("Organization ID must not be null", exception.getMessage());
    }
}
