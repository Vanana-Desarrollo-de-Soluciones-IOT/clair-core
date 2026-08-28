package com.claircore.analytics.application.internal.inboundservices.acl;

import com.claircore.analytics.domain.model.commands.ProcessTelemetryAnalyticCommand;
import com.claircore.analytics.domain.services.KpiLiveMetricsCommandService;
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
class TelemetryAnalyticEventListenerTest {

    @Mock
    private KpiLiveMetricsCommandService kpiLiveMetricsCommandService;

    @InjectMocks
    private TelemetryAnalyticEventListener listener;

    @Test
    void shouldProcessTelemetryAnalyticWhenEventReceived() {
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
        ArgumentCaptor<ProcessTelemetryAnalyticCommand> commandCaptor = ArgumentCaptor.forClass(ProcessTelemetryAnalyticCommand.class);
        verify(kpiLiveMetricsCommandService).handle(commandCaptor.capture());
        ProcessTelemetryAnalyticCommand command = commandCaptor.getValue();

        assertEquals(deviceId, command.deviceId().value());
        assertEquals(now, command.recordedAt());
        assertEquals(12.5, command.pm2_5());
        assertEquals(450.0, command.co2());
    }
}
