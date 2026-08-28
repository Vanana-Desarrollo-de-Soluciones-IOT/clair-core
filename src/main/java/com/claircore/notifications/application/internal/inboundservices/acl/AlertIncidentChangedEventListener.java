package com.claircore.notifications.application.internal.inboundservices.acl;

import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.events.AlertIncidentChangedEvent;
import com.claircore.alerting.interfaces.acl.AlertDetails;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalAlertingService;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.notifications.domain.model.entities.PushNotificationLog;
import com.claircore.notifications.domain.repositories.PushNotificationHistoryRepository;
import com.claircore.notifications.domain.services.PushNotificationDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AlertIncidentChangedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertIncidentChangedEventListener.class);

    private final ExternalAlertingService externalAlertingService;
    private final ExternalDeviceService externalDeviceService;
    private final PushNotificationDeliveryService pushNotificationDeliveryService;
    private final PushNotificationHistoryRepository pushNotificationHistoryRepository;

    public AlertIncidentChangedEventListener(
            ExternalAlertingService externalAlertingService,
            ExternalDeviceService externalDeviceService,
            PushNotificationDeliveryService pushNotificationDeliveryService,
            PushNotificationHistoryRepository pushNotificationHistoryRepository
    ) {
        this.externalAlertingService = externalAlertingService;
        this.externalDeviceService = externalDeviceService;
        this.pushNotificationDeliveryService = pushNotificationDeliveryService;
        this.pushNotificationHistoryRepository = pushNotificationHistoryRepository;
    }

    @EventListener
    public void onAlertIncidentChanged(AlertIncidentChangedEvent event) {
        LOGGER.info("Notifications BC received AlertIncidentChangedEvent for alert {}", event.alertId());
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
                            pushNotificationHistoryRepository.save(PushNotificationLog.sent(userId, alert.alertId(), title, message));
                            LOGGER.info("Push notification sent successfully via OneSignal to user {} for alert {} (status={})", 
                                    userId, alert.alertId(), alert.status());
                        } catch (Exception e) {
                            pushNotificationHistoryRepository.save(PushNotificationLog.failed(userId, alert.alertId(), title, message, e.getMessage()));
                            LOGGER.error("Failed to send push notification to user {} for alert {}: {}", userId, alert.alertId(), e.getMessage());
                        }
                    } else {
                        LOGGER.warn("Could not retrieve alert details for alertId {}, skipping push notification", event.alertId());
                    }
                } else {
                    LOGGER.warn("No owner user found for device {}, skipping push notification", deviceId);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process alert incident event for notification, alert ID {}", event.alertId(), e);
        }
    }
}
