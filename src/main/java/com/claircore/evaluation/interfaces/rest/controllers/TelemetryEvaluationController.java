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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evaluations")
@Tag(name = "Evaluations", description = "Telemetry storage endpoints")
public class TelemetryEvaluationController {

    private final TelemetryEvaluationCommandService telemetryEvaluationCommandService;
    private final TelemetryEvaluationQueryService telemetryEvaluationQueryService;
    private final ExternalDeviceService externalDeviceService;

    public TelemetryEvaluationController(
            TelemetryEvaluationCommandService telemetryEvaluationCommandService,
            TelemetryEvaluationQueryService telemetryEvaluationQueryService,
            ExternalDeviceService externalDeviceService
    ) {
        this.telemetryEvaluationCommandService = telemetryEvaluationCommandService;
        this.telemetryEvaluationQueryService = telemetryEvaluationQueryService;
        this.externalDeviceService = externalDeviceService;
    }

    @PostMapping("/telemetry")
    @Operation(summary = "Receive and store optimized telemetry from an edge device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Telemetry stored"),
            @ApiResponse(responseCode = "400", description = "Invalid telemetry data"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing API key")
    })
    public ResponseEntity<TelemetryEvaluationResponse> evaluateTelemetry(
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody EvaluateTelemetryRequest request
    ) {
        var deviceId = externalDeviceService.fetchDeviceIdByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));

        var recordedAt = Instant.now();
        if (request.created_at() != null && !request.created_at().isBlank()) {
            try {
                recordedAt = Instant.parse(request.created_at());
            } catch (java.time.format.DateTimeParseException e) {
                // Device sent an unparsable timestamp; use server time
            }
        }

        var command = new EvaluateTelemetryCommand(
                deviceId,
                request.timestamp(),
                request.uptime(),
                new AirQuality(
                        request.airQuality().co2(),
                        request.airQuality().temperature(),
                        request.airQuality().humidity()
                ),
                new ParticulateMatter(
                        request.particulateMatter().pm1_0(),
                        request.particulateMatter().pm2_5(),
                        request.particulateMatter().pm10()
                ),
                new Connectivity(
                        request.connectivity().status()
                ),
                request.status(),
                recordedAt
        );

        TelemetryEvaluation evaluation = telemetryEvaluationCommandService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TelemetryEvaluationTransform.toResponse(evaluation));
    }

    @GetMapping("/devices/{deviceId}")
    @Operation(summary = "Get stored telemetry records for a device")
    public ResponseEntity<Page<TelemetryEvaluationResponse>> getEvaluationsByDevice(
            @PathVariable UUID deviceId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        var query = new GetEvaluationsByDeviceQuery(deviceId, page, size);
        Page<TelemetryEvaluation> evaluations = telemetryEvaluationQueryService.handle(query);
        return ResponseEntity.ok(evaluations.map(TelemetryEvaluationTransform::toResponse));
    }

    @GetMapping("/devices/{deviceId}/latest")
    @Operation(summary = "Get the latest telemetry record for a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest record returned"),
            @ApiResponse(responseCode = "404", description = "No records found for device")
    })
    public ResponseEntity<TelemetryEvaluationResponse> getLatestEvaluationByDevice(
            @PathVariable UUID deviceId
    ) {
        var query = new GetLatestEvaluationByDeviceQuery(deviceId);
        return telemetryEvaluationQueryService.handle(query)
                .map(e -> ResponseEntity.ok(TelemetryEvaluationTransform.toResponse(e)))
                .orElse(ResponseEntity.notFound().build());
    }
}
