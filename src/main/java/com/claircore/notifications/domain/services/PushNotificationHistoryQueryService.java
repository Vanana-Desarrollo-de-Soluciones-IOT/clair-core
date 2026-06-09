package com.claircore.notifications.domain.services;

import com.claircore.notifications.domain.model.entities.PushNotificationLog;
import com.claircore.notifications.domain.model.queries.GetPushNotificationHistoryQuery;
import org.springframework.data.domain.Page;

public interface PushNotificationHistoryQueryService {
    Page<PushNotificationLog> handle(GetPushNotificationHistoryQuery query);
}
