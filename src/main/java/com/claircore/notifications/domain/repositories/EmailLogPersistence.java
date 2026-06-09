package com.claircore.notifications.domain.repositories;

import com.claircore.notifications.domain.model.entities.EmailLog;

public interface EmailLogPersistence {
    EmailLog save(EmailLog emailLog);
}
