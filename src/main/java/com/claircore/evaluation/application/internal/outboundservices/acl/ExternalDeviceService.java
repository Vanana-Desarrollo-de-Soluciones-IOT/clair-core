package com.claircore.evaluation.application.internal.outboundservices.acl;

import com.claircore.device.interfaces.acl.DeviceContextFacade;
import com.claircore.evaluation.domain.model.valueobjects.DeviceId;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ExternalDeviceService {

    private final DeviceContextFacade deviceContextFacade;

    public ExternalDeviceService(DeviceContextFacade deviceContextFacade) {
        this.deviceContextFacade = deviceContextFacade;
    }

    public Optional<DeviceId> fetchDeviceIdByApiKey(String apiKey) {
        return deviceContextFacade
                .findDeviceIdByApiKey(apiKey)
                .map(DeviceId::new);
    }
}
