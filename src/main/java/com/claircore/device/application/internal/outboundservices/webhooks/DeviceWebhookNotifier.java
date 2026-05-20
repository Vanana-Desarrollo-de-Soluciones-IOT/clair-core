package com.claircore.device.application.internal.outboundservices.webhooks;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.interfaces.rest.resources.DeviceWebhookNotificationResource;
import com.claircore.device.interfaces.rest.resources.ProvisionedDeviceResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class DeviceWebhookNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceWebhookNotifier.class);

    private final RestClient restClient;
    private final String deviceWebhookBaseUrl;
    private final String edgeToken;

    private static final String DEVICES_WEBHOOK_PATH = "/api/v1/provisioning/devices/events";

    public DeviceWebhookNotifier(
            RestClient.Builder restClientBuilder,
            @Value("${edge.webhook.devices-url:}") String deviceWebhookBaseUrl,
            @Value("${edge.provisioning.token:}") String edgeToken) {
        this.restClient = restClientBuilder.build();
        this.deviceWebhookBaseUrl = deviceWebhookBaseUrl;
        this.edgeToken = edgeToken;
    }

    public void notifyDeviceChanged(DeviceAssignment assignment) {
        send("DeviceChanged", toResponse(assignment, assignment.getStatus()));
    }

    public void notifyDeviceDeleted(DeviceAssignment assignment) {
        send("DeviceAssignmentDeleted", toResponse(assignment, DeviceStatus.DECOMMISSIONED));
    }

    private void send(String eventType, ProvisionedDeviceResource device) {
        if (deviceWebhookBaseUrl == null || deviceWebhookBaseUrl.isBlank()) {
            return;
        }

        try {
            var url = deviceWebhookBaseUrl;
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            var req = restClient.post().uri(url + DEVICES_WEBHOOK_PATH);
            if (edgeToken != null && !edgeToken.isBlank()) {
                req = req.header("X-Edge-Token", edgeToken);
            }
            req.body(new DeviceWebhookNotificationResource(eventType, device))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exc) {
            LOGGER.warn("Device webhook notification failed for device {}", device.id(), exc);
        }
    }

    private ProvisionedDeviceResource toResponse(DeviceAssignment assignment, DeviceStatus status) {
        Device device = assignment.getDevice();
        return new ProvisionedDeviceResource(
                device.getId().toString(),
                device.getHardwareId().value(),
                device.getApiKey().value(),
                device.getDeviceSecret().value(),
                status.name()
        );
    }

    public void notifyDeviceUnassigned(Device device) {
        send("DeviceChanged", new ProvisionedDeviceResource(
                device.getId().toString(),
                device.getHardwareId().value(),
                device.getApiKey().value(),
                device.getDeviceSecret().value(),
                DeviceStatus.OFFLINE.name()
        ));
    }
}
