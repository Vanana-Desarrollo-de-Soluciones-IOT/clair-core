package com.claircore.alerting.interfaces.rest.controllers;

import com.claircore.alerting.application.commandservices.AlertCommandService;
import com.claircore.alerting.application.queryservices.AlertQueryService;
import com.claircore.alerting.domain.model.commands.AcknowledgeEdgeAlertCommand;
import com.claircore.alerting.domain.model.queries.GetPendingEdgeAlertsQuery;
import com.claircore.alerting.interfaces.rest.resources.EdgeAlertAckRequest;
import com.claircore.alerting.interfaces.rest.resources.EdgeAlertResource;
import com.claircore.alerting.interfaces.rest.transform.EdgeAlertResourceFromEntityAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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
            @RequestParam(required = false) String since,
            @RequestParam(defaultValue = "200") int limit) {
        var query = new GetPendingEdgeAlertsQuery(parse(since), limit);
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

    private static Instant parse(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }
}
