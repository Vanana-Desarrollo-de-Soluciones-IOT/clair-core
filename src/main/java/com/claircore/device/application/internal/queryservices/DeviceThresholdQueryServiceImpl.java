package com.claircore.device.application.internal.queryservices;

import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.entities.DeviceThreshold;
import com.claircore.device.domain.model.queries.GetDeviceThresholdByMetricQuery;
import com.claircore.device.domain.model.queries.GetDeviceThresholdsByDeviceQuery;
import com.claircore.device.domain.services.DeviceThresholdQueryService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceThresholdRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceThresholdQueryServiceImpl implements DeviceThresholdQueryService {

    private final DeviceThresholdRepository deviceThresholdRepository;
    private final DeviceAssignmentRepository deviceAssignmentRepository;

    public DeviceThresholdQueryServiceImpl(
            DeviceThresholdRepository deviceThresholdRepository,
            DeviceAssignmentRepository deviceAssignmentRepository) {
        this.deviceThresholdRepository = deviceThresholdRepository;
        this.deviceAssignmentRepository = deviceAssignmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceThreshold> handle(GetDeviceThresholdsByDeviceQuery query) {
        DeviceAssignment assignment = deviceAssignmentRepository
                .findByDeviceId(query.deviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        if (assignment.getOwnerUserId() == null || !assignment.getOwnerUserId().equals(query.userId())) {
            throw new AccessDeniedException("Device does not belong to user");
        }

        return deviceThresholdRepository.findByAssignmentId(assignment.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceThreshold> handle(GetDeviceThresholdByMetricQuery query) {
        return deviceThresholdRepository.findByAssignmentIdAndMetric(query.assignmentId(), query.metric());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceThreshold> findByAssignmentId(UUID assignmentId) {
        return deviceThresholdRepository.findByAssignmentId(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceThreshold> findEnabledByAssignmentId(UUID assignmentId) {
        return deviceThresholdRepository.findEnabledByAssignmentId(assignmentId);
    }
}