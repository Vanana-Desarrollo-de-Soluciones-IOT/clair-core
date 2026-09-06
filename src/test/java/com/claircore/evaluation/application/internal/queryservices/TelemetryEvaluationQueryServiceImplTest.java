package com.claircore.evaluation.application.internal.queryservices;

import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.queries.GetEvaluationsByDeviceQuery;
import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
import com.claircore.evaluation.domain.model.valueobjects.*;
import com.claircore.evaluation.domain.repositories.TelemetryEvaluationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.claircore.shared.domain.model.PageResult;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryEvaluationQueryServiceImplTest {

    @Mock
    private TelemetryEvaluationRepository telemetryEvaluationRepository;

    @InjectMocks
    private TelemetryEvaluationQueryServiceImpl telemetryEvaluationQueryService;

    @Test
    void shouldReturnPageOfEvaluationsWhenQueryingByDevice() {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        var query = new GetEvaluationsByDeviceQuery(deviceId, 0, 10);
        TelemetryEvaluation evaluation = new TelemetryEvaluation(
                new DeviceId(deviceId), LocalTime.NOON, 3600L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(10, 15, 25),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"),
                85, "STABLE", Instant.now()
        );
        var expectedPage = new PageResult<>(List.of(evaluation), 0, 10, 1L);

        when(telemetryEvaluationRepository.findByDeviceId(deviceId, 0, 10)).thenReturn(expectedPage);

        // Act
        PageResult<TelemetryEvaluation> result = telemetryEvaluationQueryService.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.items()).hasSize(1);
        assertThat(result.total()).isEqualTo(1L);
        assertThat(result.items().get(0).getDeviceId().value()).isEqualTo(deviceId);
    }

    @Test
    void shouldReturnLatestEvaluationWhenQueryingLatestByDevice() {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        var query = new GetLatestEvaluationByDeviceQuery(deviceId);
        TelemetryEvaluation evaluation = new TelemetryEvaluation(
                new DeviceId(deviceId), LocalTime.NOON, 3600L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(10, 15, 25),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"),
                85, "STABLE", Instant.now()
        );

        when(telemetryEvaluationRepository.findLatestByDeviceId(deviceId))
                .thenReturn(Optional.of(evaluation));

        // Act
        Optional<TelemetryEvaluation> result = telemetryEvaluationQueryService.handle(query);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getDeviceId().value()).isEqualTo(deviceId);
    }
}
