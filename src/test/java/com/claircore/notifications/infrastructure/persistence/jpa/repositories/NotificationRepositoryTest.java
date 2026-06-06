package com.claircore.notifications.infrastructure.persistence.jpa.repositories;

import com.claircore.notifications.domain.model.entities.EmailLog;
import com.claircore.notifications.domain.model.entities.PushNotificationLog;
import com.claircore.notifications.domain.model.valueobjects.EmailContent;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.domain.model.valueobjects.EmailSubject;
import com.claircore.notifications.domain.repositories.EmailLogPersistence;
import com.claircore.notifications.domain.repositories.PushNotificationHistoryRepository;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(JpaAuditingConfiguration.class)
class NotificationRepositoryTest {

    @Autowired
    private EmailLogRepository emailLogRepository;

    @Autowired
    private PushNotificationLogRepository pushNotificationLogRepository;

    @Test
    void shouldFindEmailLogsByRecipientEmail() {
        var recipient = new EmailRecipient("user@example.com");
        ((EmailLogPersistence) emailLogRepository).save(EmailLog.sent(recipient, new EmailSubject("Welcome"), new EmailContent("<p>Hello</p>")));

        var logs = emailLogRepository.findByRecipientEmail(recipient);

        assertEquals(1, logs.size());
        assertEquals("user@example.com", logs.getFirst().getRecipientEmail());
    }

    @Test
    void shouldFindPushNotificationLogsByUserId() {
        UUID userId = UUID.randomUUID();
        ((PushNotificationHistoryRepository) pushNotificationLogRepository).save(PushNotificationLog.sent(userId, UUID.randomUUID(), "Title", "Message"));

        var page = pushNotificationLogRepository.findByUserId(userId, org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertTrue(page.getContent().getFirst().isSent());
    }
}
