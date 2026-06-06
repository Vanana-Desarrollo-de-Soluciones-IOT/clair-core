package com.claircore.notifications.interfaces.rest.controllers;

import com.claircore.notifications.domain.model.entities.PushNotificationLog;
import com.claircore.notifications.domain.services.PushNotificationHistoryQueryService;
import com.claircore.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter;
import com.claircore.shared.interfaces.rest.exceptions.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PushNotificationHistoryQueryService pushNotificationHistoryQueryService;

    @Test
    void shouldReturnNotificationHistoryForAuthenticatedUser() throws Exception {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655442000");
        PushNotificationLog log = PushNotificationLog.sent(userId, UUID.randomUUID(), "Alert title", "Alert message");
        ReflectionTestUtils.setField(log, "createdAt", new Date());
        ReflectionTestUtils.setField(log, "updatedAt", new Date());
        Page<PushNotificationLog> page = new PageImpl<>(List.of(log));
        when(pushNotificationHistoryQueryService.handle(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications/push")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, userId)
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Alert title"))
                .andExpect(jsonPath("$.content[0].status").value("SENT"));

        verify(pushNotificationHistoryQueryService).handle(any());
    }
}
