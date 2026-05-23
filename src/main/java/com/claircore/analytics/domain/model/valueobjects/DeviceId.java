package com.claircore.analytics.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record DeviceId(UUID value) {
    public DeviceId {
        if (value == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
    }
}
