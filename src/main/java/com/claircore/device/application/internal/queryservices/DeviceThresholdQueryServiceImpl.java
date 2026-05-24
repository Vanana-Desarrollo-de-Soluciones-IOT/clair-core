package com.claircore.device.application.internal.queryservices;

import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.queries.GetDeviceThresholdByMetricQuery;
import com.claircore.device.domain.model.queries.GetDeviceThresholdsByDeviceQuery;
import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.domain.services.DeviceThresholdQueryService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class DeviceThresholdQueryServiceImpl implements DeviceThresholdQueryService {

    private final DeviceAssignmentRepository deviceAssignmentRepository;
    private final ObjectMapper objectMapper;

    public DeviceThresholdQueryServiceImpl(
            DeviceAssignmentRepository deviceAssignmentRepository,
            ObjectMapper objectMapper) {
        this.deviceAssignmentRepository = deviceAssignmentRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceMetricThresholdConfiguration> handle(GetDeviceThresholdsByDeviceQuery query) {
        DeviceAssignment assignment = deviceAssignmentRepository
                .findByDeviceId(query.deviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        if (assignment.getOwnerUserId() == null || !assignment.getOwnerUserId().equals(query.userId())) {
            throw new AccessDeniedException("Device does not belong to user");
        }

        return Stream.of(MetricThreshold.values())
                .map(metric -> assignment.findConfigurationValue(thresholdConfigKey(metric))
                        .flatMap(this::deserializeOptional)
                        .orElse(null))
                .filter(t -> t != null)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceMetricThresholdConfiguration> handle(GetDeviceThresholdByMetricQuery query) {
        return deviceAssignmentRepository.findById(query.assignmentId())
                .flatMap(a -> a.findConfigurationValue(thresholdConfigKey(query.metric())))
                .flatMap(this::deserializeOptional);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceMetricThresholdConfiguration> findAllByAssignmentId(UUID assignmentId) {
        return deviceAssignmentRepository.findById(assignmentId)
                .map(a -> Stream.of(MetricThreshold.values())
                        .map(metric -> a.findConfigurationValue(thresholdConfigKey(metric))
                                .flatMap(this::deserializeOptional)
                                .orElse(null))
                        .filter(t -> t != null)
                        .toList())
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceMetricThresholdConfiguration> findEnabledByAssignmentId(UUID assignmentId) {
        return findAllByAssignmentId(assignmentId).stream()
                .filter(DeviceMetricThresholdConfiguration::enabled)
                .toList();
    }

    private Optional<DeviceMetricThresholdConfiguration> deserializeOptional(String rawJson) {
        try {
            return Optional.of(objectMapper.readValue(rawJson, DeviceMetricThresholdConfiguration.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String thresholdConfigKey(MetricThreshold metric) {
        return "threshold." + metric.name();
    }
}
