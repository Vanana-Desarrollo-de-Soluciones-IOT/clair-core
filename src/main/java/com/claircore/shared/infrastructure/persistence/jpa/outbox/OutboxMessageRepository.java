package com.claircore.shared.infrastructure.persistence.jpa.outbox;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {
    Page<OutboxMessage> findByPublishedAtIsNullOrderByCreatedAtAsc(Pageable pageable);
}
