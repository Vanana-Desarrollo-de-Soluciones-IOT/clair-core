package com.claircore.device.domain.model.queries;

import java.util.UUID;

public record GetDeviceByIdQuery(UUID deviceId) {
    public GetDeviceByIdQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
    }
}