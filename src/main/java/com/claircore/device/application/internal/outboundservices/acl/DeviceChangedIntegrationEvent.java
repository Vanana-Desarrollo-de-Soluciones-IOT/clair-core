package com.claircore.device.application.internal.outboundservices.acl;

/**
 * Outbound integration event published to Kafka topic
 * {@code clair.provisioning.devices.changed}.
 */
public record DeviceChangedIntegrationEvent(
        String deviceId,
        String hardwareId,
        String apiKey,
        String status,
        String changeType,
        String changedAt
) {
}
