package com.claircore.shared.infrastructure.persistence.jpa.outbox;

import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "outbox_message",
        indexes = {
                @Index(name = "idx_outbox_unpublished", columnList = "published_at, created_at")
        }
)
public class OutboxMessage extends AuditableModel {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "message_key", nullable = false)
    private String messageKey;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxMessage() {
        // JPA
    }

    public OutboxMessage(String topic, String messageKey, String payload) {
        if (topic == null || topic.isBlank()) throw new IllegalArgumentException("topic is required");
        if (messageKey == null || messageKey.isBlank()) throw new IllegalArgumentException("messageKey is required");
        if (payload == null || payload.isBlank()) throw new IllegalArgumentException("payload is required");

        this.id = UUID.randomUUID();
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.publishedAt = null;
    }

    public UUID getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt == null ? Instant.now() : publishedAt;
    }
}
