package com.claircore.device.application.internal.commandservices;

import com.claircore.device.domain.model.commands.UpdateDevicePresenceStatusCommand;
import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.services.DevicePresenceCommandService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DevicePresenceCommandServiceImpl implements DevicePresenceCommandService {

    private final DeviceRepository deviceRepository;
    private final DeviceAssignmentRepository deviceAssignmentRepository;

    public DevicePresenceCommandServiceImpl(
            DeviceRepository deviceRepository,
            DeviceAssignmentRepository deviceAssignmentRepository) {
        this.deviceRepository = deviceRepository;
        this.deviceAssignmentRepository = deviceAssignmentRepository;
    }

    @Override
    @Transactional
    public DeviceAssignment handle(UpdateDevicePresenceStatusCommand command) {
        Device device = command.deviceId() != null
                ? deviceRepository.findById(command.deviceId())
                    .orElseThrow(() -> new IllegalArgumentException("Device not found"))
                : deviceRepository.findByHardwareId(command.hardwareId().value())
                    .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        if (command.hardwareId() != null && !device.getHardwareId().equals(command.hardwareId())) {
            throw new IllegalArgumentException("Device ID and hardware ID do not match");
        }

        DeviceAssignment assignment = deviceAssignmentRepository
                .findByDeviceId(device.getId())
                .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        assignment.updatePresence(command.status(), command.occurredAt());
        return deviceAssignmentRepository.save(assignment);
    }
}
