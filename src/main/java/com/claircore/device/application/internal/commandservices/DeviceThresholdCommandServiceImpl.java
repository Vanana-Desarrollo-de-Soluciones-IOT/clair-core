package com.claircore.device.application.internal.commandservices;

import com.claircore.device.domain.model.commands.RemoveDeviceThresholdCommand;
import com.claircore.device.domain.model.commands.WriteDeviceThresholdCommand;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.services.DeviceThresholdCommandService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceThresholdCommandServiceImpl implements DeviceThresholdCommandService {

    private final DeviceAssignmentRepository deviceAssignmentRepository;

    public DeviceThresholdCommandServiceImpl(
            DeviceAssignmentRepository deviceAssignmentRepository) {
        this.deviceAssignmentRepository = deviceAssignmentRepository;
    }

    @Override
    @Transactional
    public DeviceMetricThresholdConfiguration handle(WriteDeviceThresholdCommand command) {
        DeviceAssignment assignment = loadOwnedAssignment(command.deviceId(), command.userId());

        boolean exists = assignment.getThresholds().stream()
                .anyMatch(t -> t.metric().equals(command.metric()));

        switch (command.intent()) {
            case CREATE -> {
                if (exists) throw new IllegalArgumentException("Threshold already exists for the specified metric");
            }
            case UPDATE -> {
                if (!exists) throw new IllegalArgumentException("Threshold not found for the specified metric");
            }
        }

        var configuration = new DeviceMetricThresholdConfiguration(
                command.metric(),
                command.value(),
                command.enabled()
        );

        assignment.updateThreshold(configuration);
        deviceAssignmentRepository.save(assignment);
        return configuration;
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

    @Override
    @Transactional
    public void handle(RemoveDeviceThresholdCommand command) {
        DeviceAssignment assignment = loadOwnedAssignment(command.deviceId(), command.userId());

        boolean exists = assignment.getThresholds().stream()
                .anyMatch(t -> t.metric().equals(command.metric()));
        if (!exists) throw new IllegalArgumentException("Threshold not found for the specified metric");

        assignment.removeThreshold(command.metric());
        deviceAssignmentRepository.save(assignment);
    }

    @Override
    public Optional<DeviceMetricThresholdConfiguration> findByDeviceAndMetric(UUID deviceId, MetricThreshold metric) {
        return deviceAssignmentRepository.findByDeviceId(deviceId)
                .flatMap(a -> a.getThresholds().stream()
                        .filter(t -> t.metric().equals(metric))
                        .findFirst());
    }
}
