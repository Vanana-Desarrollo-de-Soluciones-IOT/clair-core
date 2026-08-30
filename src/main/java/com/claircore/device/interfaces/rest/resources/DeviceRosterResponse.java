package com.claircore.device.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DeviceRosterResponse(
        String watermark,
        List<DeviceRosterItem> devices,
        @JsonProperty("has_more") boolean hasMore,
        @JsonProperty("next_since") String nextSince,
        @JsonProperty("next_after_id") String nextAfterId
) {
    public record DeviceRosterItem(
            @JsonProperty("device_id") String deviceId,
            @JsonProperty("hardware_id") String hardwareId,
            @JsonProperty("api_key") String apiKey,
            String status,
            boolean deleted,
            @JsonProperty("updated_at") String updatedAt
    ) {}
}
