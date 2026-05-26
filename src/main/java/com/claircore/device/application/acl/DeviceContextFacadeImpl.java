package com.claircore.device.application.acl;

import com.claircore.device.domain.model.queries.GetDeviceByApiKeyQuery;
import com.claircore.device.domain.model.queries.GetDeviceByIdQuery;
import com.claircore.device.domain.model.queries.GetDeviceByHardwareIdQuery;
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

    @Override
    public Optional<UUID> findDeviceIdByHardwareId(String hardwareId) {
        var query = new GetDeviceByHardwareIdQuery(hardwareId);
        return deviceQueryService.handle(query)
                .map(device -> device.getId());
    }

    @Override
    public Optional<UUID> findSpaceIdByDeviceId(UUID deviceId) {
        return deviceQueryService.findSpaceIdByDeviceId(deviceId);
    }

    @Override
    public Optional<String> findHardwareIdByDeviceId(UUID deviceId) {
        var query = new GetDeviceByIdQuery(deviceId);
        return deviceQueryService.handle(query)
                .map(device -> device.getHardwareId().value());
    }

    @Override
    public boolean isDeviceOwnedByUser(UUID deviceId, UUID userId) {
        return deviceQueryService.isDeviceOwnedByUser(deviceId, userId);
    }

    @Override
    public boolean isSpaceOwnedByUser(UUID spaceId, UUID userId) {
        return deviceQueryService.isSpaceOwnedByUser(spaceId, userId);
    }
}
