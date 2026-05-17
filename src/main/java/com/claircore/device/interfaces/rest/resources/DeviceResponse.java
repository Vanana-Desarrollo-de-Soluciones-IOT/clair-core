package com.claircore.device.interfaces.rest.resources;

import com.claircore.device.domain.model.valueobjects.DeviceStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DeviceResponse(
    UUID id,
    String serialNumber,
    String name,
    DeviceStatus status,
    UUID spaceId,
    UUID ownerUserId,
    Map<String, String> configuration,
    String hardwareId,
    String apiKey,
    String deviceType,
    String claimToken,
    Instant activatedAt,
    Instant lastSeenAt,
    Instant createdAt,
    Instant updatedAt
) {}
