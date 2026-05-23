package com.claircore.device.domain.model.queries;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

public record GetLatestDeviceCommandByDeviceForUserQuery(
        UUID deviceId,
        UserId userId
) {
    public GetLatestDeviceCommandByDeviceForUserQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
    }
}

