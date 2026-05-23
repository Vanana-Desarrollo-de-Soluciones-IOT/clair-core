package com.claircore.device.application.internal.outboundservices.acl;

import com.claircore.device.infrastructure.kafka.DeviceKafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka publisher that emits DeviceCommandIssued integration events
 * to the {@code clair.device.commands.pending} topic for edge consumption.
 */
@Service
public class DeviceCommandsPendingKafkaPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceCommandsPendingKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public DeviceCommandsPendingKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DeviceCommandIssuedIntegrationEvent event) {
        LOGGER.info("Publishing pending command {} to Kafka for device {}", event.commandId(), event.deviceId());
        try {
            kafkaTemplate.send(DeviceKafkaTopics.COMMANDS_PENDING.name(), event.deviceId(), toJson(event));
        } catch (Exception e) {
            LOGGER.error("Failed to publish pending command {}", event.commandId(), e);
        }
    }

    private String toJson(DeviceCommandIssuedIntegrationEvent event) {
        return String.format(
                "{\"commandId\":\"%s\",\"deviceId\":\"%s\",\"hardwareId\":\"%s\",\"commandType\":\"%s\",\"payload\":\"%s\",\"issuedAt\":\"%s\"}",
                escape(event.commandId()),
                escape(event.deviceId()),
                escape(event.hardwareId()),
                escape(event.commandType()),
                event.payload() != null ? escape(event.payload()) : "",
                escape(event.issuedAt())
        );
    }

    private String escape(String value) {
        return value != null ? value.replace("\"", "\\\"") : "";
    }
}
