package com.claircore.evaluation.application.internal.inboundservices.acl;

import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.valueobjects.*;
import com.claircore.evaluation.domain.services.TelemetryEvaluationCommandService;
import com.claircore.evaluation.infrastructure.kafka.EvaluationKafkaTopics;
import com.claircore.evaluation.application.internal.outboundservices.acl.ExternalDeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Kafka consumer that processes TelemetryRecorded integration events
 * from the Edge and stores them as telemetry evaluations.
 */
@Service
public class TelemetryRecordedKafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryRecordedKafkaConsumer.class);

    private final TelemetryEvaluationCommandService telemetryEvaluationCommandService;
    private final ObjectMapper objectMapper;
    private final ExternalDeviceService externalDeviceService;

    public TelemetryRecordedKafkaConsumer(
            TelemetryEvaluationCommandService telemetryEvaluationCommandService,
            ObjectMapper objectMapper,
            ExternalDeviceService externalDeviceService) {
        this.telemetryEvaluationCommandService = telemetryEvaluationCommandService;
        // Edge currently publishes snake_case keys.
        this.objectMapper = objectMapper.copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.externalDeviceService = externalDeviceService;
    }

    @KafkaListener(
            topics = "clair.device.telemetry.recorded",
            groupId = "core-evaluation-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String payload) {
        TelemetryRecordedIntegrationEvent event;
        try {
            event = objectMapper.readValue(payload, TelemetryRecordedIntegrationEvent.class);
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize telemetry event payload: {}", payload, e);
            // Let the error handler drive retries/DLQ; do not commit the offset.
            throw new IllegalArgumentException("Invalid telemetry event payload", e);
        }

        LOGGER.info("Consuming telemetry record for device {}", event.deviceId());

        try {
            UUID resolvedDeviceId = resolveDeviceId(event.deviceId()).orElse(null);
            if (resolvedDeviceId == null) {
                LOGGER.warn("Skipping telemetry event: unknown device identifier {}", event.deviceId());
                return;
            }

            var command = new EvaluateTelemetryCommand(
                    new com.claircore.evaluation.domain.model.valueobjects.DeviceId(resolvedDeviceId),
                    event.deviceTime(),
                    String.valueOf(event.uptimeSeconds()),
                    new AirQuality(event.co2(), event.temperature(), event.humidity()),
                    new ParticulateMatter(event.pm1_0(), event.pm2_5(), event.pm10()),
                    new Connectivity(event.wifiStatus(), event.networkName(), event.signalStrength()),
                    new Location(event.country()),
                    event.healthStatus(),
                    event.status(),
                    Instant.parse(event.recordedAt())
            );

            telemetryEvaluationCommandService.handle(command);
        } catch (Exception e) {
            LOGGER.error("Failed to process telemetry event for device {}", event.deviceId(), e);
            // Force a retry/DLQ instead of silently advancing the offset.
            throw e;
        }
    }

    private Optional<UUID> resolveDeviceId(String deviceIdOrHardwareId) {
        if (deviceIdOrHardwareId == null || deviceIdOrHardwareId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(deviceIdOrHardwareId));
        } catch (IllegalArgumentException ignored) {
            // Edge historically sent hardwareId in the device_id field.
            return externalDeviceService.findDeviceIdByHardwareId(deviceIdOrHardwareId);
        }
    }
}
