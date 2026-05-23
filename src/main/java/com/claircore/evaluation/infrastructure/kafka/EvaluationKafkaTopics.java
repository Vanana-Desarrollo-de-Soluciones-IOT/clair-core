package com.claircore.evaluation.infrastructure.kafka;

import com.claircore.shared.infrastructure.kafka.KafkaTopicConfig;

import java.util.List;

/**
 * Topics consumed or produced by the Evaluation bounded context.
 */
public class EvaluationKafkaTopics {

    public static final KafkaTopicConfig TELEMETRY_RECORDED = new KafkaTopicConfig(
            "clair.device.telemetry.recorded",
            3,
            (short) 1,
            604_800_000L,
            "delete"
    );

    public static List<KafkaTopicConfig> all() {
        return List.of(TELEMETRY_RECORDED);
    }
}
