package com.claircore.device.interfaces.acl;

import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThresholdContextFacade {

    Optional<DeviceMetricThresholdConfiguration> findThresholdByAssignmentAndMetric(UUID assignmentId, MetricThreshold metric);

    List<DeviceMetricThresholdConfiguration> findEnabledThresholdsByAssignment(UUID assignmentId);

    List<DeviceMetricThresholdConfiguration> findAllThresholdsByAssignment(UUID assignmentId);

    /**
     * Consumer-friendly threshold access. Alerting and other contexts typically
     * operate with {@code deviceId} instead of internal assignment identifiers.
     */
    List<DeviceMetricThresholdConfiguration> findEnabledThresholdsByDeviceId(UUID deviceId);

    boolean assignmentExists(UUID assignmentId);
}
