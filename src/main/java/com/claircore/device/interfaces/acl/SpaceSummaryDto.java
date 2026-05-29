package com.claircore.device.interfaces.acl;

import java.util.UUID;

public record SpaceSummaryDto(
        UUID spaceId,
        String spaceName,
        UUID organizationId
) {}

