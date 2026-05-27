package com.claircore.alerting.application.internal.outboundservices.acl;

import com.claircore.device.interfaces.acl.DeviceContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ExternalAlertingDeviceService {

    private final DeviceContextFacade deviceContextFacade;

    public ExternalAlertingDeviceService(DeviceContextFacade deviceContextFacade) {
        this.deviceContextFacade = deviceContextFacade;
    }

    public Optional<UUID> fetchSpaceIdByDeviceId(UUID deviceId) {
        return deviceContextFacade.findSpaceIdByDeviceId(deviceId);
    }

    public Optional<UUID> fetchDeviceIdByHardwareId(String hardwareId) {
        return deviceContextFacade.findDeviceIdByHardwareId(hardwareId);
    }

    public Optional<String> fetchHardwareIdByDeviceId(UUID deviceId) {
        return deviceContextFacade.findHardwareIdByDeviceId(deviceId);
    }

    public boolean verifyDeviceOwnership(UUID deviceId, UUID userId) {
        return deviceContextFacade.isDeviceOwnedByUser(deviceId, userId);
    }

    public boolean verifySpaceOwnership(UUID spaceId, UUID userId) {
        return deviceContextFacade.isSpaceOwnedByUser(spaceId, userId);
    }

    public Optional<String> fetchSpaceNameBySpaceId(UUID spaceId) {
        return deviceContextFacade.findSpaceNameBySpaceId(spaceId);
    }

    public Optional<String> fetchDeviceNameByDeviceId(UUID deviceId) {
        return deviceContextFacade.findDeviceNameByDeviceId(deviceId);
    }
}
