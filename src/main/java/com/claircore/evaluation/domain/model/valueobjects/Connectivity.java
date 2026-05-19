package com.claircore.evaluation.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record Connectivity(
        String status,
        String ssid,
        String ip,
        Integer rssi,
        String mac,
        Integer channel
) {
    public Connectivity {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be null or blank");
        }
        if (ssid == null) {
            throw new IllegalArgumentException("ssid must not be null");
        }
        if (ip == null) {
            throw new IllegalArgumentException("ip must not be null");
        }
        if (rssi == null) {
            throw new IllegalArgumentException("rssi must not be null");
        }
        if (mac == null) {
            throw new IllegalArgumentException("mac must not be null");
        }
        if (channel == null) {
            throw new IllegalArgumentException("channel must not be null");
        }
    }
}
