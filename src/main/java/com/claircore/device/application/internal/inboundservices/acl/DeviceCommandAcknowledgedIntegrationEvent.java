package com.claircore.device.application.internal.inboundservices.acl;

/**
 * Inbound integration event consumed from Kafka topic
 * {@code clair.device.commands.acknowledged}.
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
