package com.claircore.analytics.application.internal.inboundservices.acl;

import com.claircore.analytics.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.analytics.domain.model.commands.ProcessTelemetryAnalyticCommand;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.services.KpiLiveMetricsCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class TelemetryAnalyticKafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryAnalyticKafkaConsumer.class);

    private final KpiLiveMetricsCommandService kpiLiveMetricsCommandService;
    private final ObjectMapper objectMapper;
    private final ExternalDeviceService externalDeviceService;

    public TelemetryAnalyticKafkaConsumer(
            KpiLiveMetricsCommandService kpiLiveMetricsCommandService,
            ObjectMapper objectMapper,
            ExternalDeviceService externalDeviceService
    ) {
        this.kpiLiveMetricsCommandService = kpiLiveMetricsCommandService;
        this.objectMapper = objectMapper.copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.externalDeviceService = externalDeviceService;
    }

    @KafkaListener(
            topics = "clair.device.telemetry.recorded",
            groupId = "core-analytics-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String payload) {
        TelemetryRecordedIntegrationEvent event;
        try {
            event = objectMapper.readValue(payload, TelemetryRecordedIntegrationEvent.class);
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize telemetry event payload for analytics: {}", payload, e);
            return;
        }

        LOGGER.info("Analytics consuming telemetry record for device {}", event.deviceId());

        try {
            UUID deviceUuid = parseDeviceId(event.deviceId());
            if (deviceUuid == null) {
                LOGGER.warn("Skipping analytics telemetry event: invalid device identifier {}", event.deviceId());
                return;
            }

            var command = new ProcessTelemetryAnalyticCommand(
                    new DeviceId(deviceUuid),
                    event.co2(),
                    (double) event.pm2_5(),
                    event.temperature(),
                    event.humidity(),
                    Instant.parse(event.recordedAt())
            );

            kpiLiveMetricsCommandService.handle(command);
        } catch (Exception e) {
            LOGGER.error("Failed to process analytics telemetry event for device {}", event.deviceId(), e);
        }
    }

    private UUID parseDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(deviceId);
        } catch (IllegalArgumentException e) {
            return externalDeviceService.findDeviceIdByHardwareId(deviceId).orElse(null);
        }
    }
}
