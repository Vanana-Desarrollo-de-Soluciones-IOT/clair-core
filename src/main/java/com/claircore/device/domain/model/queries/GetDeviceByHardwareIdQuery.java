package com.claircore.device.domain.model.queries;

public record GetDeviceByHardwareIdQuery(String hardwareId) {
    public GetDeviceByHardwareIdQuery {
        if (hardwareId == null || hardwareId.isBlank()) {
            throw new IllegalArgumentException("Hardware ID must not be null or blank");
        }
    }
}
