package com.claircore.device.application.internal.outboundservices.acl;

import com.claircore.device.infrastructure.kafka.DeviceKafkaTopics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.claircore.shared.infrastructure.persistence.jpa.outbox.OutboxMessage;
import com.claircore.shared.infrastructure.persistence.jpa.outbox.OutboxMessageRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka publisher that emits DeviceChanged integration events
 * to the {@code clair.provisioning.devices.changed} topic for edge consumption.
 *
 * Replaces the legacy HTTP webhook notifier.
 */
@Service
public class ProvisioningDevicesChangedKafkaPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningDevicesChangedKafkaPublisher.class);

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    public ProvisioningDevicesChangedKafkaPublisher(OutboxMessageRepository outboxMessageRepository, ObjectMapper objectMapper) {
        this.outboxMessageRepository = outboxMessageRepository;
        // Edge currently expects snake_case keys.
        this.objectMapper = objectMapper.copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Transactional
    public void publish(DeviceChangedIntegrationEvent event) {
        LOGGER.info("Publishing device changed event for device {} (type={})", event.deviceId(), event.changeType());
        try {
            outboxMessageRepository.save(new OutboxMessage(
                    DeviceKafkaTopics.PROVISIONING_DEVICES_CHANGED.name(),
                    event.hardwareId(),
                    toJson(event)
            ));
        } catch (Exception e) {
            LOGGER.error("Failed to publish device changed event for device {}", event.deviceId(), e);
            throw new IllegalStateException("Failed to publish device changed integration event", e);
        }
    }

    private String toJson(DeviceChangedIntegrationEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize DeviceChangedIntegrationEvent", e);
        }
    }
}
