package com.claircore.device.application.acl;

import com.claircore.device.domain.model.queries.GetDeviceByApiKeyQuery;
import com.claircore.device.domain.model.commands.MarkDeviceSeenCommand;
import com.claircore.device.domain.services.DeviceCommandService;
import com.claircore.device.domain.services.DeviceQueryService;
import com.claircore.device.interfaces.acl.DeviceContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceContextFacadeImpl implements DeviceContextFacade {

    private final DeviceQueryService deviceQueryService;
    private final DeviceCommandService deviceCommandService;

    public DeviceContextFacadeImpl(DeviceQueryService deviceQueryService, DeviceCommandService deviceCommandService) {
        this.deviceQueryService = deviceQueryService;
        this.deviceCommandService = deviceCommandService;
    }

    @Override
    public Optional<UUID> findDeviceIdByApiKey(String apiKey) {
        var query = new GetDeviceByApiKeyQuery(apiKey);
        return deviceQueryService.handle(query)
                .map(device -> device.getId());
    }

    @Override
    public void markDeviceSeen(UUID deviceId) {
        deviceCommandService.handle(new MarkDeviceSeenCommand(deviceId));
    }
}
