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
    public boolean shouldProcess(String topic, int partition, long offset) {
        return !processedKafkaRecordRepository.existsByTopicAndPartitionAndOffset(topic, partition, offset);
    }

    @Transactional
    public void markProcessed(String topic, int partition, long offset) {
        processedKafkaRecordRepository.save(new ProcessedKafkaRecord(topic, partition, offset));
    }
}
