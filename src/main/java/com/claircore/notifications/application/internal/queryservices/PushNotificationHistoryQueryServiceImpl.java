package com.claircore.notifications.application.internal.queryservices;

import com.claircore.notifications.domain.model.entities.PushNotificationLog;
import com.claircore.notifications.domain.model.queries.GetPushNotificationHistoryQuery;
import com.claircore.notifications.domain.repositories.PushNotificationHistoryRepository;
import com.claircore.notifications.domain.services.PushNotificationHistoryQueryService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushNotificationHistoryQueryServiceImpl implements PushNotificationHistoryQueryService {

    private final PushNotificationHistoryRepository pushNotificationHistoryRepository;

    public PushNotificationHistoryQueryServiceImpl(PushNotificationHistoryRepository pushNotificationHistoryRepository) {
        this.pushNotificationHistoryRepository = pushNotificationHistoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PushNotificationLog> handle(GetPushNotificationHistoryQuery query) {
        return pushNotificationHistoryRepository.findByUserId(query.userId(), query.pageable());
    }
}
