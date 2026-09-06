package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.commands.UpdateDevicePresenceStatusCommand;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.application.commandservices.DevicePresenceCommandService;
import com.claircore.device.interfaces.rest.resources.EdgePresenceRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/edge/presence")
public class EdgePresenceController {
    private final DevicePresenceCommandService service;
    public EdgePresenceController(DevicePresenceCommandService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<Void> report(@Valid @RequestBody EdgePresenceRequest body) {
        DeviceStatus status;
        try { status = DeviceStatus.valueOf(body.status()); } catch (IllegalArgumentException ex) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid status", ex); }
        service.handle(new UpdateDevicePresenceStatusCommand(body.device_id(),
                new HardwareId(body.hardware_id()), status, body.occurred_at()));
        return ResponseEntity.ok().build();
    }
}
