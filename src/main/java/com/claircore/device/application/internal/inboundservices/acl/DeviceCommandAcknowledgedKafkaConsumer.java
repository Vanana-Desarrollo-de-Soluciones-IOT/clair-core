package com.claircore.device.application.internal.inboundservices.acl;

import com.claircore.device.domain.model.commands.AcknowledgeDeviceCommandCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.domain.services.DeviceControlCommandService;
import com.claircore.device.infrastructure.kafka.DeviceKafkaTopics;
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

    public DeviceCommandAcknowledgedKafkaConsumer(DeviceControlCommandService deviceControlCommandService) {
        this.deviceControlCommandService = deviceControlCommandService;
    }

    @KafkaListener(
            topics = "clair.device.commands.acknowledged",
            groupId = "core-device-commands-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(DeviceCommandAcknowledgedIntegrationEvent event) {
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
        }
    }
}
