package com.claircore.alerting.application.internal.eventhandlers;

import com.claircore.alerting.application.commandservices.AlertCommandService;
import com.claircore.alerting.domain.model.commands.EvaluateTelemetryForAlertsCommand;
import com.claircore.evaluation.interfaces.events.TelemetryRecordedIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Turns evaluation's published telemetry event into an alert evaluation. */
@Component
public class TelemetryRecordedEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryRecordedEventHandler.class);

    private final AlertCommandService alertCommandService;

    public TelemetryRecordedEventHandler(AlertCommandService alertCommandService) {
        this.alertCommandService = alertCommandService;
    }

    @EventListener
    public void on(TelemetryRecordedIntegrationEvent event) {
        LOGGER.info("Alerting BC received telemetry recorded event for device {}", event.deviceId());
        try {
            alertCommandService.handle(new EvaluateTelemetryForAlertsCommand(
                    event.deviceId(),
                    event.recordedAt(),
                    BigDecimal.valueOf(event.pm2_5()),
                    BigDecimal.valueOf(event.co2()),
                    BigDecimal.valueOf(event.temperature()),
                    BigDecimal.valueOf(event.humidity())
            ));
        } catch (Exception e) {
            LOGGER.error("Failed to process telemetry event for alerting, device {}", event.deviceId(), e);
        }
    }
}
