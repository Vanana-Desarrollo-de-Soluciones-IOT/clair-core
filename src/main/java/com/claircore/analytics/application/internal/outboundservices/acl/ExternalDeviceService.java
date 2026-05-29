package com.claircore.analytics.application.internal.outboundservices.acl;

import com.claircore.device.interfaces.acl.DeviceContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Consumer-side ACL for resolving Device identifiers from the Device bounded context.
 */
@Service("analyticsExternalDeviceService")
public class ExternalDeviceService {

    private final DeviceContextFacade deviceContextFacade;

    public ExternalDeviceService(DeviceContextFacade deviceContextFacade) {
        this.deviceContextFacade = deviceContextFacade;
    }

    public Optional<UUID> findDeviceIdByHardwareId(String hardwareId) {
        return deviceContextFacade.findDeviceIdByHardwareId(hardwareId);
    }

    public boolean isDeviceOwnedByUser(UUID deviceId, UUID userId) {
        return deviceContextFacade.isDeviceOwnedByUser(deviceId, userId);
    }
}
