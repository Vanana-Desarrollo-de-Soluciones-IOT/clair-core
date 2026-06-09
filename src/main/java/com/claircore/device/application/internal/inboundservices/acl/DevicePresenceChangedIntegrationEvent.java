package com.claircore.device.application.internal.inboundservices.acl;

/**
 * Inbound integration event consumed from Kafka topic
 * {@code clair.device.presence.changed}.
 */
public record DevicePresenceChangedIntegrationEvent(
        String deviceId,
        String hardwareId,
        String status,
        String occurredAt
) {
}
