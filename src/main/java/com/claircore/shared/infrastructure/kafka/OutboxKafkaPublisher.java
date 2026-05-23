package com.claircore.shared.infrastructure.kafka;

import com.claircore.shared.infrastructure.persistence.jpa.outbox.OutboxMessage;
import com.claircore.shared.infrastructure.persistence.jpa.outbox.OutboxMessageRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OutboxKafkaPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxKafkaPublisher.class);

    private final OutboxMessageRepository outboxMessageRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxKafkaPublisher(OutboxMessageRepository outboxMessageRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Small batches, frequently, to keep latency down.
    @Scheduled(fixedDelayString = "${clair.outbox.publisher.fixed-delay-ms:500}")
    @Transactional
    public void publishUnsentMessages() {
        List<OutboxMessage> batch = outboxMessageRepository
                .findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, 50))
                .getContent();

        for (OutboxMessage message : batch) {
            try {
                publishOne(message);
                message.markPublished(Instant.now());
                outboxMessageRepository.save(message);
            } catch (Exception e) {
                // Stop the batch on first failure to preserve ordering for same key/topic.
                LOGGER.error("Outbox publish failed for message {} to topic {}", message.getId(), message.getTopic(), e);
                return;
            }
        }
    }

    @Retry(name = "kafkaPublisher")
    @CircuitBreaker(name = "kafkaPublisher")
    private void publishOne(OutboxMessage message) throws Exception {
        kafkaTemplate
                .send(message.getTopic(), message.getMessageKey(), message.getPayload())
                .get(2, TimeUnit.SECONDS);
    }
}
