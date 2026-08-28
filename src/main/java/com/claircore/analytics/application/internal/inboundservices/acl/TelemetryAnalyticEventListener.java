package com.claircore.analytics.application.internal.inboundservices.acl;

import com.claircore.analytics.domain.model.commands.ProcessTelemetryAnalyticCommand;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.services.KpiLiveMetricsCommandService;
import com.claircore.evaluation.domain.model.events.TelemetryRecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TelemetryAnalyticEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryAnalyticEventListener.class);

    private final KpiLiveMetricsCommandService kpiLiveMetricsCommandService;

    public TelemetryAnalyticEventListener(KpiLiveMetricsCommandService kpiLiveMetricsCommandService) {
        this.kpiLiveMetricsCommandService = kpiLiveMetricsCommandService;
    }

    @EventListener
    public void onTelemetryRecorded(TelemetryRecordedEvent event) {
        LOGGER.info("Analytics BC received telemetry recorded event for device {}", event.deviceId());
        try {
            var command = new ProcessTelemetryAnalyticCommand(
                    new DeviceId(event.deviceId()),
                    event.co2(),
                    event.pm25(),
                    event.temperature(),
                    event.humidity(),
                    event.recordedAt()
            );

            kpiLiveMetricsCommandService.handle(command);
        } catch (Exception e) {
            LOGGER.error("Failed to process analytics telemetry event for device {}", event.deviceId(), e);
        }
    }
}
