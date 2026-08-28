package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.application.internal.inboundservices.acl.DeviceCommandAcknowledgedIntegrationEvent;
import com.claircore.device.application.internal.inboundservices.acl.DevicePresenceChangedIntegrationEvent;
import com.claircore.device.domain.model.commands.AcknowledgeDeviceCommandCommand;
import com.claircore.device.domain.model.commands.UpdateDevicePresenceStatusCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.domain.services.DeviceControlCommandService;
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
    private final DeviceControlCommandService deviceControlCommandService;

    public DeviceEdgeController(
            DevicePresenceCommandService devicePresenceCommandService,
            DeviceControlCommandService deviceControlCommandService
    ) {
        this.devicePresenceCommandService = devicePresenceCommandService;
        this.deviceControlCommandService = deviceControlCommandService;
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

    @PostMapping("/{deviceId}/commands/{commandId}/ack")
    @Operation(summary = "Acknowledge a device command execution from edge")
    public ResponseEntity<Void> acknowledgeCommand(
            @PathVariable UUID deviceId,
            @PathVariable UUID commandId,
            @Valid @RequestBody DeviceCommandAcknowledgedIntegrationEvent event
    ) {
        var command = new AcknowledgeDeviceCommandCommand(
                deviceId,
                commandId,
                DeviceCommandStatus.valueOf(event.status()),
                event.failureReason()
        );
        deviceControlCommandService.handle(command);
        return ResponseEntity.ok().build();
    }
}
