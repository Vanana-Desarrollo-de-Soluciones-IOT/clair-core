package com.claircore.device.interfaces.acl;

import java.util.UUID;

public record OrganizationSummary(
        UUID organizationId,
        String organizationName
) {}
