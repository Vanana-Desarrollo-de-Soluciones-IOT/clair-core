package com.claircore.device.interfaces.acl;

import java.util.Optional;
import java.util.UUID;

public interface DeviceContextFacade {

    Optional<UUID> findDeviceIdByApiKey(String apiKey);
}
