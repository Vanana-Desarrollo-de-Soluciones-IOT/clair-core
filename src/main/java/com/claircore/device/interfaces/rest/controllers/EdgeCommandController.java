package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceCommandRepository;
import com.claircore.device.interfaces.rest.resources.EdgeCommandAckRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/edge/commands")
public class EdgeCommandController {
    private final DeviceCommandRepository repository;
    private final ObjectMapper mapper;
    public EdgeCommandController(DeviceCommandRepository repository, ObjectMapper mapper) { this.repository = repository; this.mapper = mapper; }

    @GetMapping("/pending")
    public List<Map<String,Object>> pending(@RequestParam(required=false, name="hardware_id") String hardwareId,
                                             @RequestParam(required=false) String since,
                                             @RequestParam(defaultValue="200") int limit) {
        if (limit < 1 || limit > 500) throw new IllegalArgumentException("limit must be between 1 and 500");
        Instant instant = parse(since);
        var statuses = List.of(DeviceCommandStatus.PENDING, DeviceCommandStatus.SENT);
        var commands = hardwareId == null
                ? repository.findPendingForEdge(statuses, instant, PageRequest.of(0, limit))
                : repository.findPendingForEdgeByHardware(statuses, hardwareId, instant, PageRequest.of(0, limit));
        return commands.stream().map(this::toResource).toList();
    }

    @PostMapping("/{commandId}/ack")
    public ResponseEntity<Void> acknowledge(@PathVariable UUID commandId, @Valid @RequestBody EdgeCommandAckRequest body) {
        return repository.findById(commandId).map(command -> {
            if (command.getStatus() == DeviceCommandStatus.EXECUTED || command.getStatus() == DeviceCommandStatus.FAILED)
                return ResponseEntity.status(HttpStatus.CONFLICT).<Void>build();
            if (command.getStatus() != DeviceCommandStatus.PENDING && command.getStatus() != DeviceCommandStatus.SENT)
                return ResponseEntity.notFound().<Void>build();
            if (!command.getDevice().getHardwareId().value().equals(body.hardware_id()))
                return ResponseEntity.notFound().<Void>build();
            if (body.result() == EdgeCommandAckRequest.Result.FAILED) command.markFailed(body.detail()); else command.markExecuted();
            repository.save(command);
            return ResponseEntity.ok().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().<Void>build());
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
