package com.claircore.device.domain.services;

import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.domain.model.queries.GetDeviceCommandByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetLatestDeviceCommandByDeviceForUserQuery;

import java.util.Optional;

public interface DeviceCommandQueryService {
    Optional<DeviceCommand> handle(GetDeviceCommandByIdForUserQuery query);
    Optional<DeviceCommand> handle(GetLatestDeviceCommandByDeviceForUserQuery query);
}
