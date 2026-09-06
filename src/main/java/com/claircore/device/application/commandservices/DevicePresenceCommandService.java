package com.claircore.device.application.commandservices;

import com.claircore.device.domain.model.commands.UpdateDevicePresenceStatusCommand;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;

public interface DevicePresenceCommandService {
    DeviceAssignment handle(UpdateDevicePresenceStatusCommand command);
}
