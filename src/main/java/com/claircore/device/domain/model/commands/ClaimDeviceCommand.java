package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

public record ClaimDeviceCommand(
    String claimToken,
    UUID spaceId,
    UserId userId
) {
    public ClaimDeviceCommand {
        if (claimToken == null || claimToken.isBlank()) {
            throw new IllegalArgumentException("Claim token must not be null or blank");
        }
        if (spaceId == null) {
            throw new IllegalArgumentException("Space ID must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
    }
}
