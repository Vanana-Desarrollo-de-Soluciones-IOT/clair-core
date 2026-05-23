package com.claircore.device.application.internal.outboundservices.acl;

/**
 * Outbound integration event published to Kafka topic
 * {@code clair.device.commands.pending}.
 */
public record DeviceCommandIssuedIntegrationEvent(
        String commandId,
        String deviceId,
        String hardwareId,
        String commandType,
        String payload,
        String issuedAt
) {
}
