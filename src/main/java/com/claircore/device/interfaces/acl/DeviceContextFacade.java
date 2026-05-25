package com.claircore.device.interfaces.acl;

import java.util.Optional;
import java.util.UUID;

public interface DeviceContextFacade {

    Optional<UUID> findDeviceIdByApiKey(String apiKey);

    Optional<UUID> findDeviceIdByHardwareId(String hardwareId);

    Optional<UUID> findSpaceIdByDeviceId(UUID deviceId);

    boolean isDeviceOwnedByUser(UUID deviceId, UUID userId);

    boolean isSpaceOwnedByUser(UUID spaceId, UUID userId);
}
