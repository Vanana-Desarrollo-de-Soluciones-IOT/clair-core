package com.claircore.device.application.internal.outboundservices.acl;

import com.claircore.device.infrastructure.kafka.DeviceKafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka publisher that emits DeviceChanged integration events
 * to the {@code clair.provisioning.devices.changed} topic for edge consumption.
 *
 * Replaces the legacy HTTP webhook notifier.
 */
@Service
public class ProvisioningDevicesChangedKafkaPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningDevicesChangedKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ProvisioningDevicesChangedKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DeviceChangedIntegrationEvent event) {
        LOGGER.info("Publishing device changed event for device {} (type={})", event.deviceId(), event.changeType());
        try {
            kafkaTemplate.send(DeviceKafkaTopics.PROVISIONING_DEVICES_CHANGED.name(), event.hardwareId(), toJson(event));
        } catch (Exception e) {
            LOGGER.error("Failed to publish device changed event for device {}", event.deviceId(), e);
        }
    }

    private String toJson(DeviceChangedIntegrationEvent event) {
        return String.format(
                "{\"deviceId\":\"%s\",\"hardwareId\":\"%s\",\"apiKey\":\"%s\",\"status\":\"%s\",\"changeType\":\"%s\",\"changedAt\":\"%s\"}",
                escape(event.deviceId()),
                escape(event.hardwareId()),
                escape(event.apiKey()),
                escape(event.status()),
                escape(event.changeType()),
                escape(event.changedAt())
        );
    }

    private String escape(String value) {
        return value != null ? value.replace("\"", "\\\"") : "";
    }
}
