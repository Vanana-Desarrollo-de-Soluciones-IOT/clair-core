package com.claircore.device.application.internal.outboundservices.acl;

import com.claircore.device.infrastructure.kafka.DeviceKafkaTopics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public DeviceCommandsPendingKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
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
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize DeviceCommandIssuedIntegrationEvent", e);
        }
    }
}
