package com.claircore.alerting.interfaces.rest.controllers;

import com.claircore.alerting.domain.model.entities.Alert;
import com.claircore.alerting.domain.model.queries.GetAlertsByDeviceQuery;
import com.claircore.alerting.domain.model.queries.GetAlertsBySpaceQuery;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.services.AlertQueryService;
import com.claircore.alerting.interfaces.rest.resources.AlertResponse;
import com.claircore.alerting.interfaces.rest.resources.DailyAlertSummaryResource;
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

import java.util.Objects;
import java.util.List;
import java.util.Map;
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
            @Parameter(description = "Page size (default: 20)") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "Filter by status (e.g., ACTIVE, ACKNOWLEDGED, RESOLVED)") @RequestParam(required = false) List<AlertStatus> status) {

        UUID userId = (UUID) httpRequest.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);

        if (!externalDeviceService.verifyDeviceOwnership(deviceId, userId)) {
            throw new AccessDeniedException("Device does not belong to user");
        }

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        var query = new GetAlertsByDeviceQuery(deviceId, pageable);
        Page<Alert> alerts = (status != null && !status.isEmpty())
                ? alertQueryService.fetchByDeviceAndStatus(query, status)
                : alertQueryService.fetchByDevice(query);

        Map<UUID, String> deviceNames = externalDeviceService.fetchDeviceNamesByDeviceIds(
                alerts.getContent().stream().map(Alert::getDeviceId).distinct().toList()
        );
        Map<UUID, String> spaceNames = externalDeviceService.fetchSpaceNamesBySpaceIds(
                alerts.getContent().stream().map(Alert::getSpaceId).filter(Objects::nonNull).distinct().toList()
        );

        return ResponseEntity.ok(alerts.map(a -> AlertResponse.from(
                a,
                spaceNames.get(a.getSpaceId()),
                deviceNames.get(a.getDeviceId())
        )));
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
            @Parameter(description = "Page size (default: 20)") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "Filter by status (e.g., ACTIVE, ACKNOWLEDGED, RESOLVED)") @RequestParam(required = false) List<AlertStatus> status) {

        UUID userId = (UUID) httpRequest.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);

        if (!externalDeviceService.verifySpaceOwnership(spaceId, userId)) {
            throw new AccessDeniedException("Space does not belong to user");
        }

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        var query = new GetAlertsBySpaceQuery(spaceId, pageable);
        Page<Alert> alerts = (status != null && !status.isEmpty())
                ? alertQueryService.fetchBySpaceAndStatus(query, status)
                : alertQueryService.fetchBySpace(query);

        Map<UUID, String> deviceNames = externalDeviceService.fetchDeviceNamesByDeviceIds(
                alerts.getContent().stream().map(Alert::getDeviceId).distinct().toList()
        );
        Map<UUID, String> spaceNames = externalDeviceService.fetchSpaceNamesBySpaceIds(
                alerts.getContent().stream().map(Alert::getSpaceId).filter(Objects::nonNull).distinct().toList()
        );

        return ResponseEntity.ok(alerts.map(a -> AlertResponse.from(
                a,
                spaceNames.get(a.getSpaceId()),
                deviceNames.get(a.getDeviceId())
        )));
    }

    @GetMapping("/spaces/{spaceId}/alerts/daily-summary")
    @Operation(summary = "Get daily alert count summary for a space (last N days)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Summary returned successfully"),
            @ApiResponse(responseCode = "403", description = "Space does not belong to user")
    })
    public ResponseEntity<List<DailyAlertSummaryResource>> getDailyAlertSummary(
            HttpServletRequest httpRequest,
            @Parameter(description = "Space ID") @PathVariable UUID spaceId,
            @Parameter(description = "Number of days (default: 30)") @RequestParam(defaultValue = "30") Integer days) {

        UUID userId = (UUID) httpRequest.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);

        if (!externalDeviceService.verifySpaceOwnership(spaceId, userId)) {
            throw new AccessDeniedException("Space does not belong to user");
        }

        var summary = alertQueryService.fetchDailySummaryBySpace(spaceId, days);
        var resources = summary.stream()
                .map(d -> new DailyAlertSummaryResource(d.date(), d.count()))
                .toList();

        return ResponseEntity.ok(resources);
    }
}
