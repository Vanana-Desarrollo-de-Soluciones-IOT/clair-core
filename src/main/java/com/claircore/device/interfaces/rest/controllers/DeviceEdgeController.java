package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.application.internal.inboundservices.acl.DevicePresenceChangedIntegrationEvent;
import com.claircore.device.domain.model.commands.UpdateDevicePresenceStatusCommand;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.domain.services.DevicePresenceCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Device Edge Integration", description = "Endpoints for Edge to report device status and command execution")
public class DeviceEdgeController {

    private final DevicePresenceCommandService devicePresenceCommandService;

    public DeviceEdgeController(
            DevicePresenceCommandService devicePresenceCommandService
    ) {
        this.devicePresenceCommandService = devicePresenceCommandService;
    }

    @PostMapping("/presence/events")
    @Operation(summary = "Report device presence state change from edge")
    public ResponseEntity<Void> reportPresence(@Valid @RequestBody DevicePresenceChangedIntegrationEvent event) {
        var command = new UpdateDevicePresenceStatusCommand(
                UUID.fromString(event.deviceId()),
                new HardwareId(event.hardwareId()),
                DeviceStatus.valueOf(event.status()),
                Instant.parse(event.occurredAt())
        );
        devicePresenceCommandService.handle(command);
        return ResponseEntity.ok().build();
    }

}
