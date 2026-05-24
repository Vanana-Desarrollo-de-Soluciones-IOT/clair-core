package com.claircore.device.interfaces.acl;

import com.claircore.device.domain.model.entities.DeviceThreshold;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThresholdContextFacade {

    Optional<DeviceThreshold> findThresholdByAssignmentAndMetric(UUID assignmentId, MetricThreshold metric);

    List<DeviceThreshold> findEnabledThresholdsByAssignment(UUID assignmentId);

    List<DeviceThreshold> findAllThresholdsByAssignment(UUID assignmentId);

    boolean assignmentExists(UUID assignmentId);
}