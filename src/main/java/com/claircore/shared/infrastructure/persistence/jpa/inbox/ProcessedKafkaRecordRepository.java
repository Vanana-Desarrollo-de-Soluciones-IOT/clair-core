package com.claircore.shared.infrastructure.persistence.jpa.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedKafkaRecordRepository extends JpaRepository<ProcessedKafkaRecord, UUID> {
    boolean existsByConsumerGroupAndTopicAndPartitionAndOffset(String consumerGroup, String topic, int partition, long offset);
}
