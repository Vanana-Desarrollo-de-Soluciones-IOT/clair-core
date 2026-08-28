package com.claircore.alerting.application.internal.inboundservices.acl;

import com.claircore.alerting.domain.model.commands.EvaluateTelemetryForAlertsCommand;
import com.claircore.alerting.domain.services.AlertCommandService;
import com.claircore.evaluation.domain.model.events.TelemetryRecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AlertingTelemetryRecordedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertingTelemetryRecordedEventListener.class);

    private final AlertCommandService alertCommandService;

    public AlertingTelemetryRecordedEventListener(AlertCommandService alertCommandService) {
        this.alertCommandService = alertCommandService;
    }

    @EventListener
    public void onTelemetryRecorded(TelemetryRecordedEvent event) {
        LOGGER.info("Alerting BC received telemetry recorded event for device {}", event.deviceId());
        try {
            var command = new EvaluateTelemetryForAlertsCommand(
                    event.deviceId(),
                    event.occurredAt(),
                    BigDecimal.valueOf(event.pm25()),
                    BigDecimal.valueOf(event.co2()),
                    BigDecimal.valueOf(event.temperature()),
                    BigDecimal.valueOf(event.humidity())
            );

            alertCommandService.handle(command);
        } catch (Exception e) {
            LOGGER.error("Failed to process telemetry event for alerting, device {}", event.deviceId(), e);
        }
    }
}
