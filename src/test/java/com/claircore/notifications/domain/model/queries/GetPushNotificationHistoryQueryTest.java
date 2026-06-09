package com.claircore.notifications.domain.model.queries;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GetPushNotificationHistoryQueryTest {

    @Test
    void shouldCreateQueryWhenArgumentsAreValid() {
        UUID userId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);

        var query = new GetPushNotificationHistoryQuery(userId, pageable);

        assertEquals(userId, query.userId());
        assertEquals(pageable, query.pageable());
    }

    @Test
    void shouldRejectNullUserId() {
        var exception = assertThrowsExactly(IllegalArgumentException.class, () -> new GetPushNotificationHistoryQuery(null, PageRequest.of(0, 20)));

        assertEquals("User ID is required", exception.getMessage());
    }
}
