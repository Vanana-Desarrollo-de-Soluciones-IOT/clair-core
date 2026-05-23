package com.claircore.device.application.internal.inboundservices.acl;

import com.claircore.device.domain.model.commands.UpdateDevicePresenceStatusCommand;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.domain.services.DevicePresenceCommandService;
import com.claircore.device.infrastructure.kafka.DeviceKafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka consumer that processes DevicePresenceChanged integration events
 * from the Edge and updates local device presence status.
 */
@Service
public class DevicePresenceChangedKafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevicePresenceChangedKafkaConsumer.class);

    private final DevicePresenceCommandService devicePresenceCommandService;

    public DevicePresenceChangedKafkaConsumer(DevicePresenceCommandService devicePresenceCommandService) {
        this.devicePresenceCommandService = devicePresenceCommandService;
    }

    @KafkaListener(
            topics = "clair.device.presence.changed",
            groupId = "core-device-presence-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(DevicePresenceChangedIntegrationEvent event) {
        LOGGER.info("Consuming presence change for device {} -> {}", event.deviceId(), event.status());

        try {
            var command = new UpdateDevicePresenceStatusCommand(
                    UUID.fromString(event.deviceId()),
                    new HardwareId(event.hardwareId()),
                    DeviceStatus.valueOf(event.status()),
                    Instant.parse(event.occurredAt())
            );

            devicePresenceCommandService.handle(command);
        } catch (Exception e) {
            LOGGER.error("Failed to process presence change for device {}", event.deviceId(), e);
        }
    }
}
