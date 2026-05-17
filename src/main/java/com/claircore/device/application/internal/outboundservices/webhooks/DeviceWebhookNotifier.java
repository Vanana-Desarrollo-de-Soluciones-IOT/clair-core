package com.claircore.device.application.internal.outboundservices.webhooks;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.interfaces.rest.resources.DeviceResponse;
import com.claircore.device.interfaces.rest.resources.DeviceWebhookNotificationResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class DeviceWebhookNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceWebhookNotifier.class);

    private final RestClient restClient;
    private final String deviceWebhookUrl;

    public DeviceWebhookNotifier(
            RestClient.Builder restClientBuilder,
            @Value("${edge.webhook.devices-url:}") String deviceWebhookUrl) {
        this.restClient = restClientBuilder.build();
        this.deviceWebhookUrl = deviceWebhookUrl;
    }

    public void notifyDeviceChanged(DeviceAssignment assignment) {
        send("DeviceChanged", toResponse(assignment, assignment.getStatus()));
    }

    public void notifyDeviceDeleted(DeviceAssignment assignment) {
        send("DeviceAssignmentDeleted", toResponse(assignment, DeviceStatus.DECOMMISSIONED));
    }

    private void send(String eventType, DeviceResponse device) {
        if (deviceWebhookUrl == null || deviceWebhookUrl.isBlank()) {
            return;
        }

        try {
            restClient.post()
                .uri(deviceWebhookUrl)
                .body(new DeviceWebhookNotificationResource(eventType, device))
                .retrieve()
                .toBodilessEntity();
        } catch (RuntimeException exc) {
            LOGGER.warn("Device webhook notification failed for device {}", device.id(), exc);
        }
    }

    private DeviceResponse toResponse(DeviceAssignment assignment, DeviceStatus status) {
        Device device = assignment.getDevice();
        return new DeviceResponse(
            device.getId(),
            device.getSerialNumber(),
            device.getName(),
            status,
            assignment.getSpaceId(),
            assignment.getOwnerUserId() != null ? assignment.getOwnerUserId().userId() : null,
            assignment.getConfiguration(),
            device.getHardwareId().value(),
            device.getDeviceType().value(),
            assignment.getActivatedAt(),
            assignment.getLastSeenAt(),
            assignment.getAuditFields().getCreatedAt() != null ? assignment.getAuditFields().getCreatedAt().toInstant() : null,
            assignment.getAuditFields().getUpdatedAt() != null ? assignment.getAuditFields().getUpdatedAt().toInstant() : null
        );
    }
}
