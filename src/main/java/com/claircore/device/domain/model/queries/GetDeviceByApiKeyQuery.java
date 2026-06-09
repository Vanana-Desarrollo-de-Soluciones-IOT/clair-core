package com.claircore.device.domain.model.queries;

public record GetDeviceByApiKeyQuery(String apiKey) {
    public GetDeviceByApiKeyQuery {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key must not be null or blank");
        }
    }
}
