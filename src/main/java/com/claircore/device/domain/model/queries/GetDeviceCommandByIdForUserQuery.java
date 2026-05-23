package com.claircore.device.domain.model.queries;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

public record GetDeviceCommandByIdForUserQuery(
        UUID deviceId,
        UUID commandId,
        UserId userId
) {
    public GetDeviceCommandByIdForUserQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId must not be null");
        }
        if (commandId == null) {
            throw new IllegalArgumentException("commandId must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
    }
}

