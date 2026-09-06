package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.application.commandservices.EdgeCommandService;
import com.claircore.device.domain.model.commands.AcknowledgeEdgeCommandCommand;
import com.claircore.device.domain.model.queries.ClaimPendingEdgeCommandsQuery;
import com.claircore.device.domain.model.valueobjects.EdgeCommandResult;
import com.claircore.device.interfaces.rest.resources.EdgeCommandAckRequest;
import com.claircore.device.interfaces.rest.resources.EdgeCommandResource;
import com.claircore.device.interfaces.rest.transform.EdgeCommandResourceFromEntityAssembler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/edge/commands")
public class EdgeCommandController {

    private final EdgeCommandService edgeCommandService;
    private final ObjectMapper mapper;

    public EdgeCommandController(EdgeCommandService edgeCommandService, ObjectMapper mapper) {
        this.edgeCommandService = edgeCommandService;
        this.mapper = mapper;
    }

    @GetMapping("/pending")
    public List<EdgeCommandResource> pending(
            @RequestParam(required = false, name = "hardware_id") String hardwareId,
            @RequestParam(required = false) String since,
            @RequestParam(defaultValue = "200") int limit) {
        var query = new ClaimPendingEdgeCommandsQuery(hardwareId, parse(since), limit);
        return edgeCommandService.handle(query).stream()
                .map(pending -> EdgeCommandResourceFromEntityAssembler.toResourceFromEntity(pending, mapper))
                .toList();
    }

    @PostMapping("/{commandId}/ack")
    public ResponseEntity<Void> acknowledge(
            @PathVariable UUID commandId, @Valid @RequestBody EdgeCommandAckRequest body) {
        var command = new AcknowledgeEdgeCommandCommand(
                commandId,
                body.hardware_id(),
                body.result() == EdgeCommandAckRequest.Result.FAILED
                        ? EdgeCommandResult.FAILED : EdgeCommandResult.EXECUTED,
                body.detail());
        return switch (edgeCommandService.handle(command)) {
            case OK -> ResponseEntity.ok().build();
            case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).build();
            case NOT_FOUND -> ResponseEntity.notFound().build();
        };
    }

    private static Instant parse(String value) {
        if (value == null || value.isBlank()) return null;
        return Instant.parse(value);
    }
}
