package com.claircore.notifications.interfaces.rest.controllers;

import com.claircore.notifications.domain.model.queries.GetPushNotificationHistoryQuery;
import com.claircore.notifications.domain.services.PushNotificationHistoryQueryService;
import com.claircore.notifications.interfaces.rest.resources.PushNotificationResponse;
import com.claircore.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Notifications", description = "Push notifications management endpoints")
public class NotificationController {

    private final PushNotificationHistoryQueryService pushNotificationHistoryQueryService;

    public NotificationController(PushNotificationHistoryQueryService pushNotificationHistoryQueryService) {
        this.pushNotificationHistoryQueryService = pushNotificationHistoryQueryService;
    }

    @GetMapping("/notifications/push")
    @Operation(summary = "Get push notification logs for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications returned successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<Page<PushNotificationResponse>> getUserNotifications(
            HttpServletRequest httpRequest,
            @Parameter(description = "Page number (default: 0)") @RequestParam(defaultValue = "0") Integer page) {

        UUID userId = (UUID) httpRequest.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);

        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        var query = new GetPushNotificationHistoryQuery(userId, pageable);
        Page<PushNotificationResponse> logs = pushNotificationHistoryQueryService.handle(query)
                .map(PushNotificationResponse::from);

        return ResponseEntity.ok(logs);
    }
}
