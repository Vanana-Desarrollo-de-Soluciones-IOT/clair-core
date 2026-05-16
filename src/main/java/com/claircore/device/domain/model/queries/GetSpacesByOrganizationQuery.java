package com.claircore.device.domain.model.queries;

import java.util.UUID;

public record GetSpacesByOrganizationQuery(UUID organizationId) {
    public GetSpacesByOrganizationQuery {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID must not be null");
        }
    }
}