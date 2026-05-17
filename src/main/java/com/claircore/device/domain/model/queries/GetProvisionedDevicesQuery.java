package com.claircore.device.domain.model.queries;

public record GetProvisionedDevicesQuery(Integer limit) {
    public GetProvisionedDevicesQuery {
        if (limit == null || limit <= 0) {
            throw new IllegalArgumentException("Limit must be a positive number");
        }
    }
}
