package com.claircore.device.interfaces.rest.resources;

public record DeviceWebhookNotificationResource(
    String eventType,
    DeviceResponse device
) {
}
