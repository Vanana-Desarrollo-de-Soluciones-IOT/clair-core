package com.claircore.device.application.internal.inboundservices.acl;

import com.claircore.device.domain.model.commands.AcknowledgeDeviceCommandCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.domain.services.DeviceControlCommandService;
import com.claircore.device.infrastructure.kafka.DeviceKafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Kafka consumer that processes CommandAcknowledged integration events
 * from the Edge and updates device command status locally.
 */
@Service
public class DeviceCommandAcknowledgedKafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceCommandAcknowledgedKafkaConsumer.class);

    private final DeviceControlCommandService deviceControlCommandService;
    private final ObjectMapper objectMapper;

    public DeviceCommandAcknowledgedKafkaConsumer(DeviceControlCommandService deviceControlCommandService, ObjectMapper objectMapper) {
        this.deviceControlCommandService = deviceControlCommandService;
        // Edge currently publishes snake_case keys.
        this.objectMapper = objectMapper.copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @KafkaListener(
            topics = "clair.device.commands.acknowledged",
            groupId = "core-device-commands-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String payload) {
        DeviceCommandAcknowledgedIntegrationEvent event;
        try {
            event = objectMapper.readValue(payload, DeviceCommandAcknowledgedIntegrationEvent.class);
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize command ACK payload: {}", payload, e);
            // Let the error handler drive retries/DLQ; do not commit the offset.
            throw new IllegalArgumentException("Invalid command ACK payload", e);
        }

        LOGGER.info("Consuming command ACK for command {}", event.commandId());

        try {
            var command = new AcknowledgeDeviceCommandCommand(
                    UUID.fromString(event.deviceId()),
                    UUID.fromString(event.commandId()),
                    DeviceCommandStatus.valueOf(event.status()),
                    event.failureReason()
            );

            deviceControlCommandService.handle(command);
        } catch (Exception e) {
            LOGGER.error("Failed to process command ACK for command {}", event.commandId(), e);
            // Force a retry/DLQ instead of silently advancing the offset.
            throw e;
        }
    }
}
