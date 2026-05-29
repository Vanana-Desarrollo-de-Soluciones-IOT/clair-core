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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.claircore.shared.infrastructure.kafka.KafkaInboxService;

/**
 * Kafka consumer that processes TelemetryRecorded integration events
 * from the Edge and stores them as telemetry evaluations.
 */
@Service
public class TelemetryRecordedKafkaConsumer {

    private static final String CONSUMER_GROUP_ID = "core-evaluation-consumer";

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryRecordedKafkaConsumer.class);

    private final TelemetryEvaluationCommandService telemetryEvaluationCommandService;
    private final ObjectMapper objectMapper;
    private final ExternalDeviceService externalDeviceService;
    private final KafkaInboxService kafkaInboxService;

    // Cache for resolving hardwareId/deviceId string to UUID to avoid crossing BC boundaries on every event.
    private final Map<String, UUID> deviceIdCache = new ConcurrentHashMap<>();

    public TelemetryRecordedKafkaConsumer(
            TelemetryEvaluationCommandService telemetryEvaluationCommandService,
            ObjectMapper objectMapper,
            ExternalDeviceService externalDeviceService,
            KafkaInboxService kafkaInboxService) {
        this.telemetryEvaluationCommandService = telemetryEvaluationCommandService;
        // Edge currently publishes snake_case keys.
        this.objectMapper = objectMapper.copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.externalDeviceService = externalDeviceService;
        this.kafkaInboxService = kafkaInboxService;
    }

    @KafkaListener(
            topics = "clair.device.telemetry.recorded",
            groupId = CONSUMER_GROUP_ID,
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(ConsumerRecord<String, String> record) {
        if (!kafkaInboxService.shouldProcess(CONSUMER_GROUP_ID, record.topic(), record.partition(), record.offset())) {
            return;
        }

        String payload = record.value();
        TelemetryRecordedIntegrationEvent event;
        try {
            event = objectMapper.readValue(payload, TelemetryRecordedIntegrationEvent.class);
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize telemetry event payload: {}", payload, e);
            // Let the error handler drive retries/DLQ; do not commit the offset.
            throw new IllegalArgumentException("Invalid telemetry event payload", e);
        }

        LOGGER.debug("Consuming telemetry record for device {}", event.deviceId());

        try {
            UUID resolvedDeviceId = resolveDeviceIdCached(event.deviceId()).orElse(null);
            if (resolvedDeviceId == null) {
                LOGGER.warn("Skipping telemetry event: unknown device identifier {}", event.deviceId());
                // Skip is intentional; do not retry forever if device is unknown.
                kafkaInboxService.markProcessed(CONSUMER_GROUP_ID, record.topic(), record.partition(), record.offset());
                return;
            }

            LocalTime deviceTime;
            try {
                deviceTime = LocalTime.parse(event.deviceTime());
            } catch (DateTimeParseException | NullPointerException e) {
                LOGGER.warn("Invalid device_time format: {}. Defaulting to midnight.", event.deviceTime());
                deviceTime = LocalTime.MIDNIGHT;
            }

            var command = new EvaluateTelemetryCommand(
                    new com.claircore.evaluation.domain.model.valueobjects.DeviceId(resolvedDeviceId),
                    deviceTime,
                    event.uptimeSeconds(),
                    new AirQuality(event.co2(), event.temperature(), event.humidity()),
                    new ParticulateMatter(event.pm1_0(), event.pm2_5(), event.pm10()),
                    new Connectivity(event.wifiStatus(), event.networkName(), event.signalStrength()),
                    new Location(event.country()),
                    event.healthStatus(),
                    event.status(),
                    Instant.parse(event.recordedAt())
            );

            telemetryEvaluationCommandService.handle(command);

            kafkaInboxService.markProcessed(CONSUMER_GROUP_ID, record.topic(), record.partition(), record.offset());
        } catch (Exception e) {
            LOGGER.error("Failed to process telemetry event for device {}", event.deviceId(), e);
            // Force a retry/DLQ instead of silently advancing the offset.
            throw e;
        }
    }

    private Optional<UUID> resolveDeviceIdCached(String deviceIdOrHardwareId) {
        if (deviceIdOrHardwareId == null || deviceIdOrHardwareId.isBlank()) {
            return Optional.empty();
        }

        UUID cached = deviceIdCache.get(deviceIdOrHardwareId);
        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<UUID> resolved = resolveDeviceId(deviceIdOrHardwareId);
        resolved.ifPresent(uuid -> deviceIdCache.put(deviceIdOrHardwareId, uuid));
        return resolved;
    }

    private Optional<UUID> resolveDeviceId(String deviceIdOrHardwareId) {
        try {
            return Optional.of(UUID.fromString(deviceIdOrHardwareId));
        } catch (IllegalArgumentException ignored) {
            // Edge historically sent hardwareId in the device_id field.
            return externalDeviceService.findDeviceIdByHardwareId(deviceIdOrHardwareId);
        }
    }
}
