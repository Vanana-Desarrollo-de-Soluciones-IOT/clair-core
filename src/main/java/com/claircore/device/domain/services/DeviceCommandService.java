package com.claircore.device.domain.services;

import com.claircore.device.domain.model.commands.ClaimDeviceCommand;
import com.claircore.device.domain.model.commands.PairDeviceCommand;
import com.claircore.device.domain.model.commands.ResetDeviceAssignmentCommand;
import com.claircore.device.domain.model.commands.SeedDevicesCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceNameCommand;
import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceCommandService {
    List<Device> handle(SeedDevicesCommand command);
    DeviceAssignment handle(PairDeviceCommand command);
    DeviceAssignment handle(ClaimDeviceCommand command);
    void handle(ResetDeviceAssignmentCommand command);
    void handle(UpdateDeviceNameCommand command);
    Optional<Device> findById(UUID id);
    Optional<Device> findBySerialNumber(String serialNumber);
    Optional<Device> findByHardwareId(String hardwareId);
    Optional<Device> findByApiKey(String apiKey);
    List<Device> findBySpaceId(UUID spaceId);
    long countBySpaceId(UUID spaceId);
}
