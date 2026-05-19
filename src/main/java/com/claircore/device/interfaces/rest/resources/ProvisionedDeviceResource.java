package com.claircore.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProvisionedDeviceResource", description = "Device record used by the edge service to authenticate telemetry locally")
public record ProvisionedDeviceResource(
        @Schema(description = "clair-core device UUID")
        String id,
        @Schema(description = "Factory hardware identifier")
        String hardwareId,
        @Schema(description = "Device API key used by the edge in X-API-Key")
        String apiKey,
        @Schema(description = "Device secret used by the physical embedded device to authenticate with the edge")
        String deviceSecret,
        @Schema(description = "Lifecycle status")
        String status
) {}
