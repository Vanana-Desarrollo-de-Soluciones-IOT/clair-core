package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.application.internal.commandservices.EdgeCommandAcknowledgementService;
import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.interfaces.rest.resources.EdgeCommandAckRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/edge/commands")
public class EdgeCommandController {
    private final EdgeCommandAcknowledgementService acknowledgementService;
    private final ObjectMapper mapper;

    @Autowired
    public EdgeCommandController(EdgeCommandAcknowledgementService acknowledgementService,
                                 ObjectMapper mapper) {
        this.acknowledgementService = acknowledgementService;
        this.mapper = mapper;
    }

    @GetMapping("/pending")
    public List<Map<String,Object>> pending(@RequestParam(required=false, name="hardware_id") String hardwareId,
                                             @RequestParam(required=false) String since,
                                             @RequestParam(defaultValue="200") int limit) {
        if (limit < 1 || limit > 500) throw new IllegalArgumentException("limit must be between 1 and 500");
        Instant instant = parse(since);
        return acknowledgementService.claimForEdge(hardwareId, instant, limit)
                .stream().map(this::toResource).toList();
    }

    @PostMapping("/{commandId}/ack")
    public ResponseEntity<Void> acknowledge(@PathVariable UUID commandId, @Valid @RequestBody EdgeCommandAckRequest body) {
        return switch (acknowledgementService.acknowledge(commandId, body)) {
            case OK -> ResponseEntity.ok().<Void>build();
            case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).<Void>build();
            case NOT_FOUND -> ResponseEntity.notFound().<Void>build();
        };
    }

    private Map<String,Object> toResource(DeviceCommand c) {
        Object payload = c.getPayload();
        try { payload = mapper.readTree(c.getPayload()); } catch (Exception ignored) { }
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("command_id", c.getId().toString()); result.put("device_id", c.getDevice().getId().toString());
        result.put("hardware_id", c.getDevice().getHardwareId().value()); result.put("command_type", c.getType().name());
        result.put("payload", payload); result.put("issued_at", c.getAuditFields().getCreatedAt() == null ? "" : c.getAuditFields().getCreatedAt().toInstant().toString());
        return result;
    }
    private Instant parse(String value) { if (value == null || value.isBlank()) return null; return Instant.parse(value); }
}
