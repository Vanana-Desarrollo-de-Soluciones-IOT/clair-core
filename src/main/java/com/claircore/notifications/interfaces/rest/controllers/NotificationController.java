package com.claircore.notifications.interfaces.rest.controllers;

import com.claircore.notifications.domain.model.entities.PushNotificationLog;
import com.claircore.notifications.infrastructure.persistence.jpa.repositories.PushNotificationLogRepository;
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

    private final PushNotificationLogRepository pushNotificationLogRepository;

    public NotificationController(PushNotificationLogRepository pushNotificationLogRepository) {
        this.pushNotificationLogRepository = pushNotificationLogRepository;
    }

    @GetMapping("/notifications/push")
    @Operation(summary = "Get push notification logs for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications returned successfully")
    })
    public ResponseEntity<Page<PushNotificationLog>> getUserNotifications(
            HttpServletRequest httpRequest,
            @Parameter(description = "Page number (default: 0)") @RequestParam(defaultValue = "0") Integer page) {

        UUID userId = (UUID) httpRequest.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);

        // Always fetch the first 20 notifications per page, sorted by creation date descending.
        // The page size is fixed to 20 and not modifiable by the client.
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PushNotificationLog> logs = pushNotificationLogRepository.findByUserId(userId, pageable);

        return ResponseEntity.ok(logs);
    }
}
