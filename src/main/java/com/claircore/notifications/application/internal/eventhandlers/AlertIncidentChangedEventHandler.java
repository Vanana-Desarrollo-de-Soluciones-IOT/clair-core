package com.claircore.notifications.application.internal.eventhandlers;

import com.claircore.alerting.domain.model.events.AlertIncidentChangedEvent;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.interfaces.acl.AlertDetails;
import com.claircore.notifications.application.commandservices.PushNotificationCommandService;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalAlertingService;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.notifications.domain.model.commands.SendPushNotificationCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Turns an alerting incident into a push notification command.
 *
 * <p>Recorded deviation, to be closed in Phase 4: this still imports alerting's domain event
 * ({@code alerting.domain.model.events.AlertIncidentChangedEvent}) instead of an integration event
 * under {@code alerting.interfaces.events}, which does not exist yet.
 */
@Service
public class AlertIncidentChangedEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertIncidentChangedEventHandler.class);

    private final ExternalAlertingService externalAlertingService;
    private final ExternalDeviceService externalDeviceService;
    private final PushNotificationCommandService pushNotificationCommandService;

    public AlertIncidentChangedEventHandler(
            ExternalAlertingService externalAlertingService,
            ExternalDeviceService externalDeviceService,
            PushNotificationCommandService pushNotificationCommandService) {
        this.externalAlertingService = externalAlertingService;
        this.externalDeviceService = externalDeviceService;
        this.pushNotificationCommandService = pushNotificationCommandService;
    }

    @EventListener
    public void on(AlertIncidentChangedEvent event) {
        LOGGER.info("Notifications BC received AlertIncidentChangedEvent for alert {}", event.alertId());
        try {
            if (event.status() != AlertStatus.ACTIVE && event.status() != AlertStatus.RESOLVED) {
                return;
            }
            UUID deviceId = event.deviceId();

            Optional<UUID> ownerUserIdOpt = externalDeviceService.fetchOwnerIdByDeviceId(deviceId);
            if (ownerUserIdOpt.isEmpty()) {
                LOGGER.warn("No owner user found for device {}, skipping push notification", deviceId);
                return;
            }

            Optional<AlertDetails> alertDetailsOpt = externalAlertingService.fetchAlertDetailsById(event.alertId());
            if (alertDetailsOpt.isEmpty()) {
                LOGGER.warn("Could not retrieve alert details for alertId {}, skipping push notification", event.alertId());
                return;
            }

            AlertDetails alert = alertDetailsOpt.get();
            String deviceName = externalDeviceService.fetchDeviceNameByDeviceId(deviceId)
                    .orElse(alert.deviceName() != null ? alert.deviceName() : "Unknown Device");
            String statusLabel = alert.status().equals("ACTIVE") ? "Active Alert" : "Resolved Alert";

            pushNotificationCommandService.handle(new SendPushNotificationCommand(
                    ownerUserIdOpt.get(),
                    alert.alertId(),
                    String.format("%s: %s", statusLabel, deviceName),
                    String.format("[%s] %s", deviceName, alert.message())));
        } catch (Exception e) {
            LOGGER.error("Failed to process alert incident event for notification, alert ID {}", event.alertId(), e);
        }
    }
}
