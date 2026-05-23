package com.claircore.device.application.internal.inboundservices.acl;

import com.claircore.device.domain.model.commands.UpdateDevicePresenceStatusCommand;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.domain.services.DevicePresenceCommandService;
import com.claircore.device.infrastructure.kafka.DeviceKafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import com.claircore.shared.infrastructure.kafka.KafkaInboxService;

/**
 * Kafka consumer that processes DevicePresenceChanged integration events
 * from the Edge and updates local device presence status.
 */
@Service
public class DevicePresenceChangedKafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevicePresenceChangedKafkaConsumer.class);

    private final DevicePresenceCommandService devicePresenceCommandService;
    private final ObjectMapper objectMapper;
    private final KafkaInboxService kafkaInboxService;

    public DevicePresenceChangedKafkaConsumer(DevicePresenceCommandService devicePresenceCommandService, ObjectMapper objectMapper, KafkaInboxService kafkaInboxService) {
        this.devicePresenceCommandService = devicePresenceCommandService;
        // Edge currently publishes snake_case keys.
        this.objectMapper = objectMapper.copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.kafkaInboxService = kafkaInboxService;
    }

    @KafkaListener(
            topics = "clair.device.presence.changed",
            groupId = "core-device-presence-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(ConsumerRecord<String, String> record) {
        if (!kafkaInboxService.shouldProcess(record.topic(), record.partition(), record.offset())) {
            return;
        }

        String payload = record.value();
        DevicePresenceChangedIntegrationEvent event;
        try {
            event = objectMapper.readValue(payload, DevicePresenceChangedIntegrationEvent.class);
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize presence payload: {}", payload, e);
            // Let the error handler drive retries/DLQ; do not commit the offset.
            throw new IllegalArgumentException("Invalid presence event payload", e);
        }

        LOGGER.info("Consuming presence change for device {} -> {}", event.deviceId(), event.status());

        try {
            var command = new UpdateDevicePresenceStatusCommand(
                    UUID.fromString(event.deviceId()),
                    new HardwareId(event.hardwareId()),
                    DeviceStatus.valueOf(event.status()),
                    Instant.parse(event.occurredAt())
            );

            devicePresenceCommandService.handle(command);

            kafkaInboxService.markProcessed(record.topic(), record.partition(), record.offset());
        } catch (Exception e) {
            LOGGER.error("Failed to process presence change for device {}", event.deviceId(), e);
            // Force a retry/DLQ instead of silently advancing the offset.
            throw e;
        }
    }
}
