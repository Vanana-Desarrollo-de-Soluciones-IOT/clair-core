package com.claircore.notifications.infrastructure.persistence.jpa.repositories;

import com.claircore.notifications.domain.model.entities.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByRecipientEmail(String recipientEmail);

    Page<EmailLog> findByRecipientEmail(String recipientEmail, Pageable pageable);
}
