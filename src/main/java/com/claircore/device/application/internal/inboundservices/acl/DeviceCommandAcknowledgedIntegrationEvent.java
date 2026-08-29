package com.claircore.device.application.internal.inboundservices.acl;

/**
 * Inbound command acknowledgement integration event.
 */
public record DeviceCommandAcknowledgedIntegrationEvent(
        String deviceId,
        String hardwareId,
        String commandId,
        String status,
        String failureReason,
        String acknowledgedAt
) {
}
