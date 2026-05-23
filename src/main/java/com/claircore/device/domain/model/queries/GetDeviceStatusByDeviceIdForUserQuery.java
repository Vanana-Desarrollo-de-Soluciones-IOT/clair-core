package com.claircore.device.domain.model.queries;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

public record GetDeviceStatusByDeviceIdForUserQuery(
        UUID deviceId,
        UserId userId
) {
    public GetDeviceStatusByDeviceIdForUserQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
    }
}

