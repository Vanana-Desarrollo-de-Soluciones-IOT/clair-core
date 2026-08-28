package com.claircore.evaluation.application.internal.commandservices;

import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.valueobjects.*;
import com.claircore.evaluation.infrastructure.persistence.jpa.repositories.TelemetryEvaluationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryEvaluationCommandServiceImplTest {

    @Mock
    private TelemetryEvaluationRepository telemetryEvaluationRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TelemetryEvaluationCommandServiceImpl telemetryEvaluationCommandService;

    @Test
    void shouldSaveAndReturnTelemetryEvaluationWhenCommandIsValid() {
        // Arrange
        var command = new EvaluateTelemetryCommand(
                new DeviceId(UUID.randomUUID()),
                LocalTime.NOON,
                3600L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(10, 15, 25),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"),
                85,
                "STABLE",
                Instant.now()
        );

        TelemetryEvaluation expectedEvaluation = new TelemetryEvaluation(
                command.deviceId(), command.deviceTime(), command.uptime(),
                command.airQuality(), command.particulateMatter(),
                command.connectivity(), command.location(),
                command.healthStatus(), command.status(), command.recordedAt()
        );

        when(telemetryEvaluationRepository.save(any(TelemetryEvaluation.class))).thenReturn(expectedEvaluation);

        // Act
        TelemetryEvaluation result = telemetryEvaluationCommandService.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDeviceId()).isEqualTo(command.deviceId());
        assertThat(result.getHealthStatus()).isEqualTo(command.healthStatus());
        verify(telemetryEvaluationRepository).save(any(TelemetryEvaluation.class));
    }
}
