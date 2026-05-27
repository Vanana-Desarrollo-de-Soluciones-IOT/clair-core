package com.claircore.alerting.application.internal.outboundservices.acl;

import com.claircore.device.interfaces.acl.DeviceContextFacade;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ExternalAlertingDeviceService {

    private final DeviceContextFacade deviceContextFacade;

    public ExternalAlertingDeviceService(DeviceContextFacade deviceContextFacade) {
        this.deviceContextFacade = deviceContextFacade;
    }

    @Cacheable(value = "alerting:device-space", key = "#deviceId")
    public Optional<UUID> fetchSpaceIdByDeviceId(UUID deviceId) {
        return deviceContextFacade.findSpaceIdByDeviceId(deviceId);
    }

    public Optional<UUID> fetchDeviceIdByHardwareId(String hardwareId) {
        return deviceContextFacade.findDeviceIdByHardwareId(hardwareId);
    }

    @Cacheable(value = "alerting:device-hardware", key = "#deviceId")
    public Optional<String> fetchHardwareIdByDeviceId(UUID deviceId) {
        return deviceContextFacade.findHardwareIdByDeviceId(deviceId);
    }

    public boolean verifyDeviceOwnership(UUID deviceId, UUID userId) {
        return deviceContextFacade.isDeviceOwnedByUser(deviceId, userId);
    }

    public boolean verifySpaceOwnership(UUID spaceId, UUID userId) {
        return deviceContextFacade.isSpaceOwnedByUser(spaceId, userId);
    }

    @Cacheable(value = "alerting:space-name", key = "#spaceId")
    public Optional<String> fetchSpaceNameBySpaceId(UUID spaceId) {
        return deviceContextFacade.findSpaceNameBySpaceId(spaceId);
    }

    @Cacheable(value = "alerting:device-name", key = "#deviceId")
    public Optional<String> fetchDeviceNameByDeviceId(UUID deviceId) {
        return deviceContextFacade.findDeviceNameByDeviceId(deviceId);
    }
}
