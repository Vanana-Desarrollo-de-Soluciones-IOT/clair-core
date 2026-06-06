package com.claircore.evaluation.application.internal.queryservices;

import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.queries.GetEvaluationsByDeviceQuery;
import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
import com.claircore.evaluation.domain.model.valueobjects.*;
import com.claircore.evaluation.infrastructure.persistence.jpa.repositories.TelemetryEvaluationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        Page<TelemetryEvaluation> expectedPage = new PageImpl<>(List.of(evaluation));

        when(telemetryEvaluationRepository.findByDeviceId(eq(deviceId), any(PageRequest.class)))
                .thenReturn(expectedPage);

        // Act
        Page<TelemetryEvaluation> result = telemetryEvaluationQueryService.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeviceId().value()).isEqualTo(deviceId);
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

        when(telemetryEvaluationRepository.findFirstByDeviceIdValueOrderByRecordedAtDesc(deviceId))
                .thenReturn(Optional.of(evaluation));

        // Act
        Optional<TelemetryEvaluation> result = telemetryEvaluationQueryService.handle(query);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getDeviceId().value()).isEqualTo(deviceId);
    }
}
