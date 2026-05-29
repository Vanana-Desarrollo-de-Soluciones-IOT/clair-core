package com.claircore.device.interfaces.acl;

import java.util.UUID;

public record SpaceSummary(
        UUID spaceId,
        String spaceName,
        UUID organizationId
) {}
