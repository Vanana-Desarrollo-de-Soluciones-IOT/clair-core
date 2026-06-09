package com.claircore.device.domain.services;

import com.claircore.device.domain.model.commands.AcknowledgeDeviceCommandCommand;
import com.claircore.device.domain.model.commands.CreateDeviceCommandCommand;
import com.claircore.device.domain.model.commands.DispatchPendingDeviceCommandsCommand;
import com.claircore.device.domain.model.entities.DeviceCommand;

import java.util.List;

public interface DeviceControlCommandService {
    DeviceCommand handle(CreateDeviceCommandCommand command);
    List<DeviceCommand> handle(DispatchPendingDeviceCommandsCommand command);
    DeviceCommand handle(AcknowledgeDeviceCommandCommand command);
}
