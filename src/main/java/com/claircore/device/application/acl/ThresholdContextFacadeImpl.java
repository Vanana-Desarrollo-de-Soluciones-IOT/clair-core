package com.claircore.device.application.acl;

import com.claircore.device.domain.model.entities.DeviceThreshold;
import com.claircore.device.domain.model.queries.GetDeviceThresholdByMetricQuery;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.domain.services.DeviceThresholdQueryService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.claircore.device.interfaces.acl.ThresholdContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ThresholdContextFacadeImpl implements ThresholdContextFacade {

    private final DeviceThresholdQueryService deviceThresholdQueryService;
    private final DeviceAssignmentRepository deviceAssignmentRepository;

    public ThresholdContextFacadeImpl(
            DeviceThresholdQueryService deviceThresholdQueryService,
            DeviceAssignmentRepository deviceAssignmentRepository) {
        this.deviceThresholdQueryService = deviceThresholdQueryService;
        this.deviceAssignmentRepository = deviceAssignmentRepository;
    }

    @Override
    public Optional<DeviceThreshold> findThresholdByAssignmentAndMetric(UUID assignmentId, MetricThreshold metric) {
        var query = new GetDeviceThresholdByMetricQuery(assignmentId, metric);
        return deviceThresholdQueryService.handle(query);
    }

    @Override
    public List<DeviceThreshold> findEnabledThresholdsByAssignment(UUID assignmentId) {
        return deviceThresholdQueryService.findEnabledByAssignmentId(assignmentId);
    }

    @Override
    public List<DeviceThreshold> findAllThresholdsByAssignment(UUID assignmentId) {
        return deviceThresholdQueryService.findByAssignmentId(assignmentId);
    }

    @Override
    public boolean assignmentExists(UUID assignmentId) {
        return deviceAssignmentRepository.existsById(assignmentId);
    }
}