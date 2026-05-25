package com.claircore.alerting.domain.model.queries;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

public record GetAlertsByDeviceQuery(
        UUID deviceId,
        Pageable pageable
) {
    public GetAlertsByDeviceQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
    }
}
