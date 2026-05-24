package com.claircore.device.domain.model.queries;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

public record GetDeviceThresholdsByDeviceQuery(
        UUID deviceId,
        UserId userId
) {
    public GetDeviceThresholdsByDeviceQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
    }
}