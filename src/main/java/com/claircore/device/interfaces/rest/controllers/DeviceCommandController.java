package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.commands.AcknowledgeDeviceCommandCommand;
import com.claircore.device.domain.model.commands.CreateDeviceCommandCommand;
import com.claircore.device.domain.model.commands.DispatchPendingDeviceCommandsCommand;
import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.services.DeviceControlCommandService;
import com.claircore.device.interfaces.rest.resources.AcknowledgeDeviceCommandRequest;
import com.claircore.device.interfaces.rest.resources.CreateDeviceCommandRequest;
import com.claircore.device.interfaces.rest.resources.DeviceCommandResponse;
import com.claircore.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Device Commands", description = "Device command creation, edge dispatch, and acknowledgements")
public class DeviceCommandController {

    private final DeviceControlCommandService deviceControlCommandService;

    @Value("${edge.provisioning.token:}")
    private String edgeToken;

    public DeviceCommandController(DeviceControlCommandService deviceControlCommandService) {
        this.deviceControlCommandService = deviceControlCommandService;
    }

    @PostMapping("/{deviceId}/commands")
    @Operation(summary = "Create a command for a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Command created"),
            @ApiResponse(responseCode = "400", description = "Invalid command request"),
            @ApiResponse(responseCode = "403", description = "Device does not belong to the authenticated user"),
            @ApiResponse(responseCode = "404", description = "Device assignment not found")
    })
    public ResponseEntity<DeviceCommandResponse> createDeviceCommand(
            HttpServletRequest httpRequest,
            @PathVariable UUID deviceId,
            @Valid @RequestBody CreateDeviceCommandRequest request
    ) {
        UUID userId = (UUID) httpRequest.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        DeviceCommand command = deviceControlCommandService.handle(new CreateDeviceCommandCommand(
                deviceId,
                request.type(),
                request.payload(),
                new UserId(userId)
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(command));
    }

    @GetMapping("/commands/pending")
    @Operation(summary = "Dispatch pending commands to the edge")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending commands returned and marked as sent"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing edge token")
    })
    public ResponseEntity<List<DeviceCommandResponse>> dispatchPendingCommands(
            @RequestHeader(value = "X-Edge-Token", required = false) String providedEdgeToken,
            @RequestParam(defaultValue = "100") Integer limit
    ) {
        if (!isValidEdgeToken(providedEdgeToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<DeviceCommand> commands = deviceControlCommandService.handle(new DispatchPendingDeviceCommandsCommand(limit));
        return ResponseEntity.ok(commands.stream().map(this::toResponse).toList());
    }

    @PostMapping("/{deviceId}/commands/{commandId}/ack")
    @Operation(summary = "Acknowledge command execution from the edge")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Command acknowledgement accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid acknowledgement request"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing edge token"),
            @ApiResponse(responseCode = "404", description = "Device command not found")
    })
    public ResponseEntity<DeviceCommandResponse> acknowledgeDeviceCommand(
            @RequestHeader(value = "X-Edge-Token", required = false) String providedEdgeToken,
            @PathVariable UUID deviceId,
            @PathVariable UUID commandId,
            @Valid @RequestBody AcknowledgeDeviceCommandRequest request
    ) {
        if (!isValidEdgeToken(providedEdgeToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        DeviceCommand command = deviceControlCommandService.handle(new AcknowledgeDeviceCommandCommand(
                deviceId,
                commandId,
                request.status(),
                request.failureReason()
        ));

        return ResponseEntity.ok(toResponse(command));
    }

    private boolean isValidEdgeToken(String providedEdgeToken) {
        return edgeToken != null && !edgeToken.isBlank() && providedEdgeToken != null && edgeToken.equals(providedEdgeToken);
    }

    private DeviceCommandResponse toResponse(DeviceCommand command) {
        Instant createdAt = command.getAuditFields().getCreatedAt() != null
                ? command.getAuditFields().getCreatedAt().toInstant()
                : null;

        return new DeviceCommandResponse(
                command.getId(),
                command.getDevice().getId(),
                command.getType(),
                command.getStatus(),
                command.getPayload(),
                command.getSentAt(),
                command.getExecutedAt(),
                command.getFailureReason(),
                createdAt
        );
    }
}
