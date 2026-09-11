package com.claircore.alerting.interfaces.rest.controllers;

import com.claircore.alerting.application.commandservices.AlertCommandService;
import com.claircore.alerting.application.queryservices.AlertQueryService;
import com.claircore.alerting.domain.model.commands.AcknowledgeEdgeAlertCommand;
import com.claircore.alerting.domain.model.commands.RecordEdgeAlertReceiptCommand;
import com.claircore.alerting.domain.model.queries.GetPendingEdgeAlertsQuery;
import com.claircore.alerting.interfaces.rest.resources.EdgeAlertAckRequest;
import com.claircore.alerting.interfaces.rest.resources.EdgeAlertReceiptRequest;
import com.claircore.alerting.interfaces.rest.resources.EdgeAlertResource;
import com.claircore.alerting.interfaces.rest.transform.EdgeAlertResourceFromEntityAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/edge/alerts")
public class EdgeAlertController {

    private final AlertQueryService alertQueryService;
    private final AlertCommandService alertCommandService;

    public EdgeAlertController(AlertQueryService alertQueryService, AlertCommandService alertCommandService) {
        this.alertQueryService = alertQueryService;
        this.alertCommandService = alertCommandService;
    }

    @GetMapping("/pending")
    public List<EdgeAlertResource> pending(
            @RequestParam(name = "after_sequence", required = false) Long afterSequence,
            @RequestParam(defaultValue = "200") int limit) {
        var query = new GetPendingEdgeAlertsQuery(afterSequence, limit);
        return alertQueryService.fetchPendingForEdge(query).stream()
                .map(EdgeAlertResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @PostMapping("/{alertId}/ack")
    public ResponseEntity<Void> acknowledge(@PathVariable UUID alertId, @Valid @RequestBody EdgeAlertAckRequest request) {
        var command = new AcknowledgeEdgeAlertCommand(alertId, request.hardware_id(), request.acknowledged_at());
        return switch (alertCommandService.handle(command)) {
            case OK -> ResponseEntity.ok().<Void>build();
            case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).<Void>build();
            case NOT_FOUND -> ResponseEntity.notFound().<Void>build();
        };
    }

    /** Delivery receipt. 200 also for an older sequence (idempotent), 404 for unknown or not owned. */
    @PostMapping("/{alertId}/receipt")
    public ResponseEntity<Void> receipt(@PathVariable UUID alertId, @Valid @RequestBody EdgeAlertReceiptRequest request) {
        var command = new RecordEdgeAlertReceiptCommand(alertId, request.hardware_id(), request.sequence());
        return switch (alertCommandService.handle(command)) {
            case OK -> ResponseEntity.ok().<Void>build();
            case NOT_FOUND -> ResponseEntity.notFound().<Void>build();
        };
    }
}
