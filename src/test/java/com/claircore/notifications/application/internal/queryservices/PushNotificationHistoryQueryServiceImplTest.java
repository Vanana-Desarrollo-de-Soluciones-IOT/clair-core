package com.claircore.notifications.application.internal.queryservices;

import com.claircore.notifications.domain.model.entities.PushNotificationLog;
import com.claircore.notifications.domain.model.queries.GetPushNotificationHistoryQuery;
import com.claircore.notifications.domain.repositories.PushNotificationHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushNotificationHistoryQueryServiceImplTest {

    @Mock
    private PushNotificationHistoryRepository pushNotificationHistoryRepository;

    @InjectMocks
    private PushNotificationHistoryQueryServiceImpl service;

    @Test
    void shouldReturnPushNotificationHistoryForUser() {
        UUID userId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        Page<PushNotificationLog> page = new PageImpl<>(List.of(PushNotificationLog.sent(userId, UUID.randomUUID(), "Title", "Message")));
        when(pushNotificationHistoryRepository.findByUserId(userId, pageable)).thenReturn(page);

        var result = service.handle(new GetPushNotificationHistoryQuery(userId, pageable));

        assertEquals(1, result.getTotalElements());
        verify(pushNotificationHistoryRepository).findByUserId(userId, pageable);
    }
}
