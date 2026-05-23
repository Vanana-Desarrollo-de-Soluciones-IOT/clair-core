package com.claircore.shared.infrastructure.persistence.jpa.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedKafkaRecordRepository extends JpaRepository<ProcessedKafkaRecord, Long> {
    boolean existsByTopicAndPartitionAndOffset(String topic, int partition, long offset);
}
