package com.claircore.evaluation.interfaces.rest.controllers;

import com.claircore.evaluation.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.queries.GetEvaluationsByDeviceQuery;
import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
import com.claircore.evaluation.domain.model.valueobjects.*;
import com.claircore.evaluation.domain.services.TelemetryEvaluationCommandService;
import com.claircore.evaluation.domain.services.TelemetryEvaluationQueryService;
import com.claircore.evaluation.interfaces.rest.resources.EvaluateTelemetryRequest;
import com.claircore.evaluation.interfaces.rest.resources.TelemetryEvaluationResponse;
import com.claircore.evaluation.interfaces.rest.transform.TelemetryEvaluationTransform;
import com.claircore.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evaluations")
@Tag(name = "Evaluations", description = "Telemetry storage endpoints")
public class TelemetryEvaluationController {

    private final TelemetryEvaluationQueryService telemetryEvaluationQueryService;
    private final TelemetryEvaluationCommandService telemetryEvaluationCommandService;
    private final ExternalDeviceService externalDeviceService;

    public TelemetryEvaluationController(
            TelemetryEvaluationQueryService telemetryEvaluationQueryService,
            TelemetryEvaluationCommandService telemetryEvaluationCommandService,
            ExternalDeviceService externalDeviceService
    ) {
        this.telemetryEvaluationQueryService = telemetryEvaluationQueryService;
        this.telemetryEvaluationCommandService = telemetryEvaluationCommandService;
        this.externalDeviceService = externalDeviceService;
    }

    @PostMapping("/telemetry")
    @Operation(summary = "Store telemetry record from edge device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Record stored successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    public ResponseEntity<TelemetryEvaluationResponse> evaluateTelemetry(@Valid @RequestBody EvaluateTelemetryRequest request) {
        UUID deviceId = resolveDeviceId(request.deviceId()).orElse(null);
        if (deviceId == null) return ResponseEntity.notFound().build();

        LocalTime deviceTime;
        try {
            deviceTime = LocalTime.parse(request.timestamp());
        } catch (DateTimeParseException e) {
            deviceTime = LocalTime.now();
        }

        Long uptimeSeconds;
        try {
            // Edge may send "20" or "00:00:20". If it's a number, use it directly.
            uptimeSeconds = Long.parseLong(request.uptime());
        } catch (NumberFormatException e) {
            uptimeSeconds = 0L; // Fallback or more complex parsing if needed
        }

        Instant recordedAt = request.created_at() != null ? Instant.parse(request.created_at()) : Instant.now();

        var command = new EvaluateTelemetryCommand(
                new DeviceId(deviceId),
                deviceTime,
                uptimeSeconds,
                new AirQuality(request.airQuality().co2(), request.airQuality().temperature(), request.airQuality().humidity()),
                new ParticulateMatter(request.particulateMatter().pm1_0(), request.particulateMatter().pm2_5(), request.particulateMatter().pm10()),
                new Connectivity(request.connectivity().status(), request.connectivity().network(), request.connectivity().signalStrength()),
                new Location(request.location().country()),
                request.healthStatus(),
                request.status(),
                recordedAt
        );

        TelemetryEvaluation evaluation = telemetryEvaluationCommandService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(TelemetryEvaluationTransform.toResponse(evaluation));
    }

    @GetMapping("/devices/{deviceId}")
    @Operation(summary = "Get stored telemetry records for a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Telemetry records returned"),
            @ApiResponse(responseCode = "403", description = "Access denied: User does not own the device"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<TelemetryEvaluationResponse>> getEvaluationsByDevice(
            HttpServletRequest httpRequest,
            @PathVariable UUID deviceId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        UUID userId = (UUID) httpRequest.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        if (userId == null || !externalDeviceService.isDeviceOwnedByUser(deviceId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var query = new GetEvaluationsByDeviceQuery(deviceId, page, size);
        Page<TelemetryEvaluation> evaluations = telemetryEvaluationQueryService.handle(query);
        return ResponseEntity.ok(evaluations.map(TelemetryEvaluationTransform::toResponse));
    }

    @GetMapping("/devices/{deviceId}/latest")
    @Operation(summary = "Get the latest telemetry record for a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest record returned"),
            @ApiResponse(responseCode = "403", description = "Access denied: User does not own the device"),
            @ApiResponse(responseCode = "404", description = "No records found for device")
    })
    public ResponseEntity<TelemetryEvaluationResponse> getLatestEvaluationByDevice(
            HttpServletRequest httpRequest,
            @PathVariable UUID deviceId
    ) {
        UUID userId = (UUID) httpRequest.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        if (userId == null || !externalDeviceService.isDeviceOwnedByUser(deviceId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var query = new GetLatestEvaluationByDeviceQuery(deviceId);
        return telemetryEvaluationQueryService.handle(query)
                .map(e -> ResponseEntity.ok(TelemetryEvaluationTransform.toResponse(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    private java.util.Optional<UUID> resolveDeviceId(String deviceIdOrHardwareId) {
        try {
            return java.util.Optional.of(UUID.fromString(deviceIdOrHardwareId));
        } catch (IllegalArgumentException e) {
            return externalDeviceService.findDeviceIdByHardwareId(deviceIdOrHardwareId);
        }
    }
}
