package com.claircore.device.domain.services;

import com.claircore.device.domain.model.commands.UpdateDevicePresenceStatusCommand;
import com.claircore.device.domain.model.entities.DeviceAssignment;

public interface DevicePresenceCommandService {
    DeviceAssignment handle(UpdateDevicePresenceStatusCommand command);
}
