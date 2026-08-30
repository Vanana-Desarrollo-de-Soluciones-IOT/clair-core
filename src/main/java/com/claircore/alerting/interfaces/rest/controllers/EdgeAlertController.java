package com.claircore.alerting.interfaces.rest.controllers;

import com.claircore.alerting.domain.model.entities.Alert;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.infrastructure.persistence.jpa.repositories.AlertRepository;
import com.claircore.alerting.application.internal.commandservices.EdgeAlertAcknowledgementService;
import com.claircore.alerting.interfaces.rest.resources.EdgeAlertAckRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/edge/alerts")
public class EdgeAlertController {
    private static final List<AlertStatus> EDGE_DELIVERY_STATUSES =
            List.of(AlertStatus.ACTIVE, AlertStatus.RESOLVED);

    private final AlertRepository repository;
    private final EdgeAlertAcknowledgementService acknowledgementService;
    public EdgeAlertController(AlertRepository repository, EdgeAlertAcknowledgementService acknowledgementService) {
        this.repository = repository;
        this.acknowledgementService = acknowledgementService;
    }
    @GetMapping("/pending")
    public List<Map<String,Object>> pending(@RequestParam(required=false) String since, @RequestParam(defaultValue="200") int limit) {
        if (limit < 1 || limit > 500) throw new IllegalArgumentException("limit must be between 1 and 500");
        return repository.findPendingForEdge(EDGE_DELIVERY_STATUSES, parse(since), PageRequest.of(0, limit)).stream().map(p -> resource(p.getAlert(), p.getHardwareId())).toList();
    }
    @PostMapping("/{alertId}/ack")
    public ResponseEntity<Void> acknowledge(@PathVariable UUID alertId, @Valid @RequestBody EdgeAlertAckRequest request) {
        return switch (acknowledgementService.acknowledge(alertId, request)) {
            case OK -> ResponseEntity.ok().<Void>build();
            case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).<Void>build();
            case NOT_FOUND -> ResponseEntity.notFound().<Void>build();
        };
    }
    private Map<String,Object> resource(Alert a, String hardwareId) {
        Map<String,Object> r = new LinkedHashMap<>(); r.put("alert_id", a.getId().toString()); r.put("device_id", a.getDeviceId().toString()); r.put("hardware_id", hardwareId); r.put("metric", a.getMetric().name()); r.put("threshold_value", a.getThresholdValue()); r.put("actual_value", a.getActualValue()); r.put("message", a.getMessage()); r.put("status", a.getStatus().name()); r.put("occurred_at", a.getOccurredAt().toString()); r.put("resolved_at", a.getResolvedAt() == null ? null : a.getResolvedAt().toString()); return r;
    }
    private Instant parse(String value) { return value == null || value.isBlank() ? null : Instant.parse(value); }
}
