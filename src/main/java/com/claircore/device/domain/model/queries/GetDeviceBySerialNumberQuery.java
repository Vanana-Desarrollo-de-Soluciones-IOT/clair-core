package com.claircore.device.domain.model.queries;

public record GetDeviceBySerialNumberQuery(String serialNumber) {
    public GetDeviceBySerialNumberQuery {
        if (serialNumber == null || serialNumber.isBlank()) {
            throw new IllegalArgumentException("Serial number must not be null or blank");
        }
    }
}