package com.claircore.evaluation.interfaces.rest.controllers;

import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.queries.GetEvaluationsByDeviceQuery;
import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
import com.claircore.evaluation.domain.services.TelemetryEvaluationQueryService;
import com.claircore.evaluation.interfaces.rest.resources.TelemetryEvaluationResponse;
import com.claircore.evaluation.interfaces.rest.transform.TelemetryEvaluationTransform;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evaluations")
@Tag(name = "Evaluations", description = "Telemetry storage endpoints")
public class TelemetryEvaluationController {

    private final TelemetryEvaluationQueryService telemetryEvaluationQueryService;

    public TelemetryEvaluationController(
            TelemetryEvaluationQueryService telemetryEvaluationQueryService
    ) {
        this.telemetryEvaluationQueryService = telemetryEvaluationQueryService;
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
