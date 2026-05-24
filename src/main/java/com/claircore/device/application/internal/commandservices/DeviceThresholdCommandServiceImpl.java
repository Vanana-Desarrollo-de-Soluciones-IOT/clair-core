package com.claircore.device.application.internal.commandservices;

import com.claircore.device.domain.model.commands.RemoveDeviceThresholdCommand;
import com.claircore.device.domain.model.commands.WriteDeviceThresholdCommand;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.entities.DeviceThreshold;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.services.DeviceThresholdCommandService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceThresholdRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceThresholdCommandServiceImpl implements DeviceThresholdCommandService {

    private final DeviceThresholdRepository deviceThresholdRepository;
    private final DeviceAssignmentRepository deviceAssignmentRepository;

    public DeviceThresholdCommandServiceImpl(
            DeviceThresholdRepository deviceThresholdRepository,
            DeviceAssignmentRepository deviceAssignmentRepository) {
        this.deviceThresholdRepository = deviceThresholdRepository;
        this.deviceAssignmentRepository = deviceAssignmentRepository;
    }

    @Override
    @Transactional
    public DeviceThreshold handle(WriteDeviceThresholdCommand command) {
        DeviceAssignment assignment = loadOwnedAssignment(command.deviceId(), command.userId());

        return switch (command.intent()) {
            case CREATE -> createThreshold(assignment, command);
            case UPDATE -> updateThreshold(assignment, command);
        };
    }

    private DeviceAssignment loadOwnedAssignment(UUID deviceId, UserId userId) {
        DeviceAssignment assignment = deviceAssignmentRepository
                .findByDeviceId(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        if (assignment.getOwnerUserId() == null || !assignment.getOwnerUserId().equals(userId)) {
            throw new AccessDeniedException("Device does not belong to user");
        }

        return assignment;
    }

    private DeviceThreshold createThreshold(DeviceAssignment assignment, WriteDeviceThresholdCommand command) {
        boolean exists = deviceThresholdRepository
                .findByAssignmentIdAndMetric(assignment.getId(), command.metric())
                .isPresent();

        if (exists) {
            throw new IllegalArgumentException("Threshold already exists for the specified metric");
        }

        DeviceThreshold threshold = new DeviceThreshold(
                assignment.getId(),
                command.metric(),
                command.operator(),
                command.value()
        );

        threshold.toggle(command.enabled());
        return deviceThresholdRepository.save(threshold);
    }

    private DeviceThreshold updateThreshold(DeviceAssignment assignment, WriteDeviceThresholdCommand command) {
        DeviceThreshold threshold = deviceThresholdRepository
                .findByAssignmentIdAndMetric(assignment.getId(), command.metric())
                .orElseThrow(() -> new IllegalArgumentException("Threshold not found for the specified metric"));

        threshold.update(command.operator(), command.value());
        threshold.toggle(command.enabled());
        return deviceThresholdRepository.save(threshold);
    }

    @Override
    @Transactional
    public void handle(RemoveDeviceThresholdCommand command) {
        DeviceAssignment assignment = loadOwnedAssignment(command.deviceId(), command.userId());

        DeviceThreshold threshold = deviceThresholdRepository
                .findByAssignmentIdAndMetric(assignment.getId(), command.metric())
                .orElseThrow(() -> new IllegalArgumentException("Threshold not found for the specified metric"));

        deviceThresholdRepository.delete(threshold);
    }

    @Override
    public Optional<DeviceThreshold> findById(UUID id) {
        return deviceThresholdRepository.findById(id);
    }
}
