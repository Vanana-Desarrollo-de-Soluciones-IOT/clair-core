package com.claircore.device.application.queryservices;

import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.queries.GetDeviceCommandByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetLatestDeviceCommandByDeviceForUserQuery;

import java.util.Optional;

public interface DeviceCommandQueryService {
    Optional<DeviceCommand> handle(GetDeviceCommandByIdForUserQuery query);
    Optional<DeviceCommand> handle(GetLatestDeviceCommandByDeviceForUserQuery query);
}
