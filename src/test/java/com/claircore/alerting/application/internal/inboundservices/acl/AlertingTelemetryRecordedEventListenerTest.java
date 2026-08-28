package com.claircore.alerting.application.internal.inboundservices.acl;

import com.claircore.alerting.domain.model.commands.EvaluateTelemetryForAlertsCommand;
import com.claircore.alerting.domain.services.AlertCommandService;
import com.claircore.evaluation.domain.model.events.TelemetryRecordedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlertingTelemetryRecordedEventListenerTest {

    @Mock
    private AlertCommandService alertCommandService;

    @InjectMocks
    private AlertingTelemetryRecordedEventListener listener;

    @Test
    void shouldEvaluateTelemetryForAlertsWhenEventReceived() {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        Instant now = Instant.now();
        TelemetryRecordedEvent event = new TelemetryRecordedEvent(
                deviceId,
                "HW-01",
                450.0,
                12.5,
                23.5,
                52.0,
                now,
                now,
                25.0,
                5.0,
                "connected",
                "Wokwi-GUEST",
                -65,
                "PERU",
                "100",
                "Optimal",
                20L,
                "14:30:25"
        );

        // Act
        listener.onTelemetryRecorded(event);

        // Assert
        ArgumentCaptor<EvaluateTelemetryForAlertsCommand> commandCaptor = ArgumentCaptor.forClass(EvaluateTelemetryForAlertsCommand.class);
        verify(alertCommandService).handle(commandCaptor.capture());
        EvaluateTelemetryForAlertsCommand command = commandCaptor.getValue();

        assertEquals(deviceId, command.deviceId());
        assertEquals(now, command.occurredAt());
        assertEquals(12.5, command.pm25().doubleValue());
        assertEquals(450.0, command.co2().doubleValue());
    }
}
