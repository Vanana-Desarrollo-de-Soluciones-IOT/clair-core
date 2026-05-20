package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.commands.UpdateDevicePresenceStatusCommand;
import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.domain.services.DevicePresenceCommandService;
import com.claircore.device.interfaces.rest.resources.DevicePresenceEventRequest;
import com.claircore.device.interfaces.rest.resources.DeviceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices/presence")
@Tag(name = "Device Presence", description = "Edge-reported device presence synchronization")
public class DevicePresenceController {

    private final DevicePresenceCommandService devicePresenceCommandService;

    @Value("${edge.provisioning.token:}")
    private String edgeToken;

    public DevicePresenceController(DevicePresenceCommandService devicePresenceCommandService) {
        this.devicePresenceCommandService = devicePresenceCommandService;
    }

    @PostMapping("/events")
    @Operation(summary = "Update device presence from the edge")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Presence event accepted",
                    content = @Content(schema = @Schema(implementation = DeviceResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid presence event"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing edge token"),
            @ApiResponse(responseCode = "404", description = "Device assignment not found")
    })
    public ResponseEntity<DeviceResponse> updateDevicePresence(
            @RequestHeader(value = "X-Edge-Token", required = false) String providedEdgeToken,
            @Valid @RequestBody DevicePresenceEventRequest request) {

        if (!isValidEdgeToken(providedEdgeToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        DeviceAssignment assignment = devicePresenceCommandService.handle(new UpdateDevicePresenceStatusCommand(
                request.deviceId(),
                new HardwareId(request.hardwareId()),
                request.status(),
                request.occurredAt()
        ));

        return ResponseEntity.ok(toResponse(assignment));
    }

    private boolean isValidEdgeToken(String providedEdgeToken) {
        return edgeToken != null && !edgeToken.isBlank() && providedEdgeToken != null && edgeToken.equals(providedEdgeToken);
    }

    private DeviceResponse toResponse(DeviceAssignment assignment) {
        Device device = assignment.getDevice();
        return new DeviceResponse(
                device.getId(),
                device.getSerialNumber(),
                device.getName(),
                assignment.getStatus(),
                assignment.getSpaceId(),
                assignment.getOwnerUserId() != null ? assignment.getOwnerUserId().userId() : null,
                assignment.getConfiguration(),
                device.getHardwareId().value(),
                device.getDeviceType().value(),
                assignment.getActivatedAt(),
                assignment.getLastSeenAt(),
                assignment.getAuditFields().getCreatedAt() != null ? assignment.getAuditFields().getCreatedAt().toInstant() : null,
                assignment.getAuditFields().getUpdatedAt() != null ? assignment.getAuditFields().getUpdatedAt().toInstant() : null
        );
    }
}
