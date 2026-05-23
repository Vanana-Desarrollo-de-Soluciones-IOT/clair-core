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
                @UniqueConstraint(name = "uq_processed_kafka_record", columnNames = {"topic", "kafka_partition", "kafka_offset"})
        },
        indexes = {
                @Index(name = "idx_processed_kafka_record_ts", columnList = "processed_at")
        }
)
public class ProcessedKafkaRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public ProcessedKafkaRecord(String topic, int partition, long offset) {
        if (topic == null || topic.isBlank()) throw new IllegalArgumentException("topic is required");
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.processedAt = Instant.now();
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
