package com.claircore.device.domain.model.queries;

import java.util.UUID;

public record GetOrganizationByIdQuery(UUID organizationId) {
    public GetOrganizationByIdQuery {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID must not be null");
        }
    }
}