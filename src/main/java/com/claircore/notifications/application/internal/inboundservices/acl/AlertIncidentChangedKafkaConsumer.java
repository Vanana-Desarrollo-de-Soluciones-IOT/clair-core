package com.claircore.notifications.application.internal.inboundservices.acl;

import com.claircore.alerting.application.internal.outboundservices.acl.AlertIncidentChangedIntegrationEvent;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.interfaces.acl.AlertDetails;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalAlertingService;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.notifications.domain.model.entities.PushNotificationLog;
import com.claircore.notifications.domain.services.PushNotificationDeliveryService;
import com.claircore.notifications.infrastructure.persistence.jpa.repositories.PushNotificationLogRepository;
import com.claircore.shared.infrastructure.kafka.KafkaInboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AlertIncidentChangedKafkaConsumer {

    private static final String CONSUMER_GROUP_ID = "core-notifications-alert-consumer";
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertIncidentChangedKafkaConsumer.class);

    private final ExternalAlertingService externalAlertingService;
    private final ExternalDeviceService externalDeviceService;
    private final PushNotificationDeliveryService pushNotificationDeliveryService;
    private final PushNotificationLogRepository pushNotificationLogRepository;
    private final KafkaInboxService kafkaInboxService;
    private final ObjectMapper objectMapper;

    public AlertIncidentChangedKafkaConsumer(
            ExternalAlertingService externalAlertingService,
            ExternalDeviceService externalDeviceService,
            PushNotificationDeliveryService pushNotificationDeliveryService,
            PushNotificationLogRepository pushNotificationLogRepository,
            KafkaInboxService kafkaInboxService,
            ObjectMapper objectMapper
    ) {
        this.externalAlertingService = externalAlertingService;
        this.externalDeviceService = externalDeviceService;
        this.pushNotificationDeliveryService = pushNotificationDeliveryService;
        this.pushNotificationLogRepository = pushNotificationLogRepository;
        this.kafkaInboxService = kafkaInboxService;
        this.objectMapper = objectMapper.copy()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @KafkaListener(
            topics = "clair.device.alert.incident.changed",
            groupId = CONSUMER_GROUP_ID,
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(ConsumerRecord<String, String> record) {
        if (!kafkaInboxService.shouldProcess(CONSUMER_GROUP_ID, record.topic(), record.partition(), record.offset())) {
            return;
        }

        String payload = record.value();
        AlertIncidentChangedIntegrationEvent event;
        try {
            event = objectMapper.readValue(payload, AlertIncidentChangedIntegrationEvent.class);
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize alert incident event payload: {}", payload, e);
            throw new IllegalArgumentException("Invalid alert event payload", e);
        }

        try {
            if (event.status() == AlertStatus.ACTIVE || event.status() == AlertStatus.RESOLVED) {
                UUID deviceId = event.deviceId();
                
                Optional<UUID> ownerUserIdOpt = externalDeviceService.fetchOwnerIdByDeviceId(deviceId);
                if (ownerUserIdOpt.isPresent()) {
                    UUID userId = ownerUserIdOpt.get();
                    
                    Optional<AlertDetails> alertDetailsOpt = externalAlertingService.fetchAlertDetailsById(event.alertId());
                    if (alertDetailsOpt.isPresent()) {
                        AlertDetails alert = alertDetailsOpt.get();
                        String deviceName = externalDeviceService.fetchDeviceNameByDeviceId(deviceId)
                                .orElse(alert.deviceName() != null ? alert.deviceName() : "Unknown Device");

                        String statusLabel = alert.status().equals("ACTIVE") ? "Active Alert" : "Resolved Alert";
                        
                        String title = String.format("%s: %s", statusLabel, deviceName);
                        String message = String.format("[%s] %s", deviceName, alert.message());
                        
                        try {
                            pushNotificationDeliveryService.sendPushNotification(userId, title, message);
                            pushNotificationLogRepository.save(PushNotificationLog.sent(userId, alert.alertId(), title, message));
                            LOGGER.info("Push notification sent successfully via OneSignal to user {} for alert {} (status={})", 
                                    userId, alert.alertId(), alert.status());
                        } catch (Exception e) {
                            pushNotificationLogRepository.save(PushNotificationLog.failed(userId, alert.alertId(), title, message, e.getMessage()));
                            LOGGER.error("Failed to send push notification to user {} for alert {}: {}", userId, alert.alertId(), e.getMessage());
                        }
                    } else {
                        LOGGER.warn("Could not retrieve alert details for alertId {}, skipping push notification", event.alertId());
                    }
                } else {
                    LOGGER.warn("No owner user found for device {}, skipping push notification", deviceId);
                }
            }
            kafkaInboxService.markProcessed(CONSUMER_GROUP_ID, record.topic(), record.partition(), record.offset());
        } catch (Exception e) {
            LOGGER.error("Failed to process alert incident event for notification, alert ID {}", event.alertId(), e);
            throw e;
        }
    }
}
