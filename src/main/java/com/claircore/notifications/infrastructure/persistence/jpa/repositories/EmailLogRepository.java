package com.claircore.notifications.infrastructure.persistence.jpa.repositories;

import com.claircore.notifications.domain.model.entities.EmailLog;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.domain.repositories.EmailLogPersistence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, UUID>, EmailLogPersistence {
    List<EmailLog> findByRecipientEmail(EmailRecipient recipientEmail);

    Page<EmailLog> findByRecipientEmail(EmailRecipient recipientEmail, Pageable pageable);
}
