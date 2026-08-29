package com.claircore.device.application.internal.inboundservices.acl;

/**
 * Inbound device presence integration event.
 */
public record DevicePresenceChangedIntegrationEvent(
        String deviceId,
        String hardwareId,
        String status,
        String occurredAt
) {
}
