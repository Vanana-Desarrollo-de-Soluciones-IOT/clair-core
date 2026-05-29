package com.claircore.device.interfaces.acl;

import java.util.UUID;

public record OrganizationSummaryDto(
        UUID organizationId,
        String organizationName
) {}

