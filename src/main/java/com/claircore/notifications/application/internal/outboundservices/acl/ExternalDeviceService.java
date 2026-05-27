package com.claircore.notifications.application.internal.outboundservices.acl;

import com.claircore.device.interfaces.acl.DeviceContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service("notificationsExternalDeviceService")
public class ExternalDeviceService {

    private final DeviceContextFacade deviceContextFacade;

    public ExternalDeviceService(DeviceContextFacade deviceContextFacade) {
        this.deviceContextFacade = deviceContextFacade;
    }

    public Optional<UUID> fetchOwnerIdByDeviceId(UUID deviceId) {
        return deviceContextFacade.findOwnerIdByDeviceId(deviceId);
    }

    public Optional<String> fetchDeviceNameByDeviceId(UUID deviceId) {
        return deviceContextFacade.findDeviceNameByDeviceId(deviceId);
    }
}

