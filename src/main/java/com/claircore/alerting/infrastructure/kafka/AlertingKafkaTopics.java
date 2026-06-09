package com.claircore.alerting.infrastructure.kafka;

import com.claircore.shared.infrastructure.kafka.KafkaTopicConfig;

import java.util.List;

/**
 * Topics consumed or produced by the Alerting bounded context.
 */
public class AlertingKafkaTopics {

    /**
     * Emitted by clair-core when an alert incident opens/closes for a device.
     * Consumed by Edge to notify embedded.
     */
    public static final KafkaTopicConfig ALERT_INCIDENT_CHANGED = new KafkaTopicConfig(
            "clair.device.alert.incident.changed",
            3,
            (short) 1,
            604_800_000L,
            "delete"
    );

    public static List<KafkaTopicConfig> all() {
        return List.of(ALERT_INCIDENT_CHANGED);
    }
}
