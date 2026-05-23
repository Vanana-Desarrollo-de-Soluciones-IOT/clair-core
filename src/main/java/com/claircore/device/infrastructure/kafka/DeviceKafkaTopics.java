package com.claircore.device.infrastructure.kafka;

import com.claircore.shared.infrastructure.kafka.KafkaTopicConfig;

import java.util.List;

/**
 * Topics consumed or produced by the Device bounded context.
 */
public class DeviceKafkaTopics {

    public static final KafkaTopicConfig COMMANDS_PENDING = new KafkaTopicConfig(
            "clair.device.commands.pending",
            3,
            (short) 1,
            604_800_000L,
            "delete"
    );

    public static final KafkaTopicConfig COMMANDS_ACKNOWLEDGED = new KafkaTopicConfig(
            "clair.device.commands.acknowledged",
            1,
            (short) 1,
            604_800_000L,
            "delete"
    );

    public static final KafkaTopicConfig DEVICE_PRESENCE_CHANGED = new KafkaTopicConfig(
            "clair.device.presence.changed",
            1,
            (short) 1,
            259_200_000L, // 3 days
            "delete"
    );

    public static final KafkaTopicConfig PROVISIONING_DEVICES_CHANGED = new KafkaTopicConfig(
            "clair.provisioning.devices.changed",
            1,
            (short) 1,
            604_800_000L,
            "delete"
    );

    public static List<KafkaTopicConfig> all() {
        return List.of(
                COMMANDS_PENDING,
                COMMANDS_ACKNOWLEDGED,
                DEVICE_PRESENCE_CHANGED,
                PROVISIONING_DEVICES_CHANGED
        );
    }
}
