package com.claircore.shared.infrastructure.persistence.jpa.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "processed_kafka_record",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_processed_kafka_record",
                        columnNames = {"consumer_group", "topic", "kafka_partition", "kafka_offset"}
                )
        },
        indexes = {
                @Index(name = "idx_processed_kafka_record_ts", columnList = "processed_at")
        }
)
public class ProcessedKafkaRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @Column(name = "consumer_group", nullable = false, length = 200)
    private String consumerGroup;

    @Column(nullable = false)
    private String topic;

    @Column(name = "kafka_partition", nullable = false)
    private int partition;

    @Column(name = "kafka_offset", nullable = false)
    private long offset;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedKafkaRecord() {
        // JPA
    }

    public ProcessedKafkaRecord(String consumerGroup, String topic, int partition, long offset) {
        if (consumerGroup == null || consumerGroup.isBlank()) throw new IllegalArgumentException("consumerGroup is required");
        if (topic == null || topic.isBlank()) throw new IllegalArgumentException("topic is required");
        this.consumerGroup = consumerGroup;
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.processedAt = Instant.now();
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public String getTopic() {
        return topic;
    }

    public int getPartition() {
        return partition;
    }

    public long getOffset() {
        return offset;
    }
}
