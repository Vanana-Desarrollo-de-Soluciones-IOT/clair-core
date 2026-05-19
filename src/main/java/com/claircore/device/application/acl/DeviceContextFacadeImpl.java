package com.claircore.device.application.acl;

import com.claircore.device.domain.model.queries.GetDeviceByApiKeyQuery;
import com.claircore.device.domain.services.DeviceQueryService;
import com.claircore.device.interfaces.acl.DeviceContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceContextFacadeImpl implements DeviceContextFacade {

    private final DeviceQueryService deviceQueryService;

    public DeviceContextFacadeImpl(DeviceQueryService deviceQueryService) {
        this.deviceQueryService = deviceQueryService;
    }

    @Override
    public Optional<UUID> findDeviceIdByApiKey(String apiKey) {
        var query = new GetDeviceByApiKeyQuery(apiKey);
        return deviceQueryService.handle(query)
                .map(device -> device.getId());
    }
}
