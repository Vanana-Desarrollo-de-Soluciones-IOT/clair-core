package com.claircore.shared.infrastructure.kafka;

import com.claircore.shared.infrastructure.persistence.jpa.inbox.ProcessedKafkaRecord;
import com.claircore.shared.infrastructure.persistence.jpa.inbox.ProcessedKafkaRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KafkaInboxService {

    private final ProcessedKafkaRecordRepository processedKafkaRecordRepository;

    public KafkaInboxService(ProcessedKafkaRecordRepository processedKafkaRecordRepository) {
        this.processedKafkaRecordRepository = processedKafkaRecordRepository;
    }

    @Transactional
    public boolean shouldProcess(String consumerGroup, String topic, int partition, long offset) {
        return !processedKafkaRecordRepository.existsByConsumerGroupAndTopicAndPartitionAndOffset(consumerGroup, topic, partition, offset);
    }

    @Transactional
    public void markProcessed(String consumerGroup, String topic, int partition, long offset) {
        processedKafkaRecordRepository.save(new ProcessedKafkaRecord(consumerGroup, topic, partition, offset));
    }
}
