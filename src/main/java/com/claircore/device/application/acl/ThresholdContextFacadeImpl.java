package com.claircore.device.application.acl;

import com.claircore.device.domain.model.queries.GetDeviceThresholdByMetricQuery;
import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.application.queryservices.DeviceThresholdQueryService;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.interfaces.acl.ThresholdContextFacade;
import com.claircore.device.interfaces.acl.ThresholdSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class ThresholdContextFacadeImpl implements ThresholdContextFacade {

    private final DeviceThresholdQueryService deviceThresholdQueryService;
    private final DeviceAssignmentRepository deviceAssignmentRepository;
    private final ObjectMapper objectMapper;

    public ThresholdContextFacadeImpl(
            DeviceThresholdQueryService deviceThresholdQueryService,
            DeviceAssignmentRepository deviceAssignmentRepository,
            ObjectMapper objectMapper) {
        this.deviceThresholdQueryService = deviceThresholdQueryService;
        this.deviceAssignmentRepository = deviceAssignmentRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<DeviceMetricThresholdConfiguration> findThresholdByAssignmentAndMetric(UUID assignmentId, MetricThreshold metric) {
        var query = new GetDeviceThresholdByMetricQuery(assignmentId, metric);
        return deviceThresholdQueryService.handle(query);
    }

    @Override
    public List<DeviceMetricThresholdConfiguration> findEnabledThresholdsByAssignment(UUID assignmentId) {
        return deviceThresholdQueryService.findEnabledByAssignmentId(assignmentId);
    }

    @Override
    public List<DeviceMetricThresholdConfiguration> findAllThresholdsByAssignment(UUID assignmentId) {
        return deviceThresholdQueryService.findAllByAssignmentId(assignmentId);
    }

    @Override
    public List<ThresholdSummary> findEnabledThresholdsByDeviceId(UUID deviceId) {
        return deviceAssignmentRepository.findByDeviceId(deviceId)
                .map(a -> Stream.of(MetricThreshold.values())
                        .map(metric -> a.findConfigurationValue(thresholdConfigKey(metric))
                                .flatMap(this::deserializeOptional)
                                .orElse(null))
                        .filter(t -> t != null && t.enabled())
                        .map(t -> new ThresholdSummary(t.metric().name(), t.value(), t.enabled()))
                        .toList())
                .orElseGet(List::of);
    }

    @Override
    public boolean assignmentExists(UUID assignmentId) {
        return deviceAssignmentRepository.findById(assignmentId).isPresent();
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
