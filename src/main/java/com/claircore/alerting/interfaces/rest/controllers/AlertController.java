package com.claircore.alerting.interfaces.rest.controllers;

import com.claircore.alerting.domain.model.entities.Alert;
import com.claircore.alerting.domain.model.queries.GetAlertsByDeviceQuery;
import com.claircore.alerting.domain.model.queries.GetAlertsBySpaceQuery;
import com.claircore.alerting.domain.services.AlertQueryService;
import com.claircore.alerting.interfaces.rest.resources.AlertResponse;
import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingDeviceService;
import com.claircore.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Alerts", description = "Alert management endpoints")
public class AlertController {

    private final AlertQueryService alertQueryService;
    private final ExternalAlertingDeviceService externalDeviceService;

    public AlertController(
            AlertQueryService alertQueryService,
            ExternalAlertingDeviceService externalDeviceService) {
        this.alertQueryService = alertQueryService;
        this.externalDeviceService = externalDeviceService;
    }

    @GetMapping("/devices/{deviceId}/alerts")
    @Operation(summary = "Get all alerts for a specific device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alerts returned successfully"),
            @ApiResponse(responseCode = "403", description = "Device does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    public ResponseEntity<Page<AlertResponse>> getAlertsByDevice(
            HttpServletRequest httpRequest,
            @Parameter(description = "Device ID") @PathVariable UUID deviceId,
            @Parameter(description = "Page number (default: 0)") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size (default: 20)") @RequestParam(defaultValue = "20") Integer size) {

        UUID userId = (UUID) httpRequest.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);

        if (!externalDeviceService.verifyDeviceOwnership(deviceId, userId)) {
            throw new AccessDeniedException("Device does not belong to user");
        }

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        var query = new GetAlertsByDeviceQuery(deviceId, pageable);
        Page<Alert> alerts = alertQueryService.fetchByDevice(query);

        return ResponseEntity.ok(alerts.map(AlertResponse::from));
    }

    @GetMapping("/spaces/{spaceId}/alerts")
    @Operation(summary = "Get all alerts for a specific space")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alerts returned successfully"),
            @ApiResponse(responseCode = "403", description = "Space does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Space not found")
    })
    public ResponseEntity<Page<AlertResponse>> getAlertsBySpace(
            HttpServletRequest httpRequest,
            @Parameter(description = "Space ID") @PathVariable UUID spaceId,
            @Parameter(description = "Page number (default: 0)") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size (default: 20)") @RequestParam(defaultValue = "20") Integer size) {

        UUID userId = (UUID) httpRequest.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);

        if (!externalDeviceService.verifySpaceOwnership(spaceId, userId)) {
            throw new AccessDeniedException("Space does not belong to user");
        }

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        var query = new GetAlertsBySpaceQuery(spaceId, pageable);
        Page<Alert> alerts = alertQueryService.fetchBySpace(query);

        return ResponseEntity.ok(alerts.map(AlertResponse::from));
    }
}
