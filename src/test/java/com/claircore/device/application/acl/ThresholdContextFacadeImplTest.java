package com.claircore.device.application.acl;

import com.claircore.device.domain.model.queries.GetDeviceThresholdByMetricQuery;
import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.domain.services.DeviceThresholdQueryService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThresholdContextFacadeImplTest {

    @Mock
    private DeviceThresholdQueryService deviceThresholdQueryService;

    @Mock
    private DeviceAssignmentRepository deviceAssignmentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDelegateFindThresholdByAssignmentAndMetricToQueryService() {
        ThresholdContextFacadeImpl facade = new ThresholdContextFacadeImpl(deviceThresholdQueryService, deviceAssignmentRepository, objectMapper);
        UUID assignmentId = UUID.fromString("550e8400-e29b-41d4-a716-446655441310");
        DeviceMetricThresholdConfiguration configuration = new DeviceMetricThresholdConfiguration(MetricThreshold.PM25, new BigDecimal("35.5"), true);
        when(deviceThresholdQueryService.handle(new GetDeviceThresholdByMetricQuery(assignmentId, MetricThreshold.PM25))).thenReturn(Optional.of(configuration));

        Optional<DeviceMetricThresholdConfiguration> result = facade.findThresholdByAssignmentAndMetric(assignmentId, MetricThreshold.PM25);

        assertEquals(Optional.of(configuration), result);
    }

    @Test
    void shouldReturnAssignmentExistsFromRepository() {
        ThresholdContextFacadeImpl facade = new ThresholdContextFacadeImpl(deviceThresholdQueryService, deviceAssignmentRepository, objectMapper);
        UUID assignmentId = UUID.fromString("550e8400-e29b-41d4-a716-446655441300");
        when(deviceAssignmentRepository.existsById(assignmentId)).thenReturn(true);

        assertEquals(true, facade.assignmentExists(assignmentId));
    }
}
