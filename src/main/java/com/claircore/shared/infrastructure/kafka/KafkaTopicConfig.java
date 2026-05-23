package com.claircore.shared.infrastructure.kafka;

/**
 * Topic configuration primitive — shared infrastructure.
 *
 * Each bounded context owns its own topic registry; this record is only
 * the portable configuration shape.
 */
public record KafkaTopicConfig(
        String name,
        int numPartitions,
        short replicationFactor,
        long retentionMs,
        String cleanupPolicy
) {
    public KafkaTopicConfig {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Topic name is required");
        }
        if (numPartitions <= 0) {
            throw new IllegalArgumentException("numPartitions must be positive");
        }
        if (replicationFactor <= 0) {
            throw new IllegalArgumentException("replicationFactor must be positive");
        }
        if (retentionMs <= 0) {
            retentionMs = 604_800_000L; // 7 days default
        }
        if (cleanupPolicy == null || cleanupPolicy.isBlank()) {
            cleanupPolicy = "delete";
        }
    }
}
