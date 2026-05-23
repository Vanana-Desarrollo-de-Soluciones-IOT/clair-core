package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.commands.ClaimDeviceCommand;
import com.claircore.device.domain.model.commands.PairDeviceCommand;
import com.claircore.device.domain.model.commands.ResetDeviceAssignmentCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceNameCommand;
import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.queries.GetDeviceByIdQuery;
import com.claircore.device.domain.model.queries.GetDevicesBySpaceQuery;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.services.DeviceCommandService;
import com.claircore.device.domain.services.DeviceQueryService;
import com.claircore.device.interfaces.rest.resources.*;
import com.claircore.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Devices", description = "Device pairing and management endpoints")
public class DeviceController {

    private final DeviceCommandService deviceCommandService;
    private final DeviceQueryService deviceQueryService;

    public DeviceController(
            DeviceCommandService deviceCommandService,
            DeviceQueryService deviceQueryService) {
        this.deviceCommandService = deviceCommandService;
        this.deviceQueryService = deviceQueryService;
    }

    @PostMapping("/pair")
    @Operation(summary = "Pair a physical device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pairing started and claim token issued"),
            @ApiResponse(responseCode = "400", description = "Device not registered in factory inventory")
    })
    public ResponseEntity<DevicePairingResource> pairDevice(@Valid @RequestBody PairDeviceRequest request) {
        var command = new PairDeviceCommand(request.hardwareId());
        DeviceAssignment assignment = deviceCommandService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new DevicePairingResource(
                        assignment.getDevice().getId(),
                        assignment.getClaimToken() != null ? assignment.getClaimToken().value() : null
                )
        );
    }

    @PostMapping("/claim")
    @Operation(summary = "Claim a device into a user-owned space")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device claimed and assigned to the requested space"),
            @ApiResponse(responseCode = "400", description = "Invalid claim token, missing fields, or device already claimed"),
            @ApiResponse(responseCode = "403", description = "The target space does not belong to the authenticated user")
    })
    public ResponseEntity<DeviceResponse> claimDevice(
            HttpServletRequest httpRequest,
            @Valid @RequestBody ClaimDeviceRequest request) {

        UUID userId = (UUID) httpRequest.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        var command = new ClaimDeviceCommand(
                request.claimToken(),
                request.spaceId(),
                new UserId(userId)
        );

        DeviceAssignment assignment = deviceCommandService.handle(command);
        return ResponseEntity.ok(toResponse(assignment));
    }

    @GetMapping
    @Operation(summary = "Get devices by space with pagination")
    public ResponseEntity<Page<DeviceResponse>> getDevices(
            @RequestParam UUID spaceId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        var query = new GetDevicesBySpaceQuery(spaceId, page, size);
        Page<DeviceAssignment> assignments = deviceQueryService.handle(query);
        return ResponseEntity.ok(assignments.map(this::toResponse));
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Get device by ID")
    public ResponseEntity<DeviceResponse> getDevice(@PathVariable UUID deviceId) {
        var query = new GetDeviceByIdQuery(deviceId);
        return deviceQueryService.handle(query)
                .map(device -> ResponseEntity.ok(toResponse(device)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Reset a device assignment for reconfiguration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Device assignment reset and space cleared"),
            @ApiResponse(responseCode = "400", description = "Device not found or invalid request"),
            @ApiResponse(responseCode = "403", description = "The device does not belong to the authenticated user")
    })
    public ResponseEntity<Void> deleteDevice(HttpServletRequest httpRequest, @PathVariable UUID deviceId) {
        UUID userId = (UUID) httpRequest.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        deviceCommandService.handle(new ResetDeviceAssignmentCommand(deviceId, new UserId(userId)));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping({"/{deviceId}/name", "/{deviceId}"})
    @Operation(summary = "Update device display name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device name updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "The device does not belong to the authenticated user")
    })
    public ResponseEntity<Void> updateDeviceName(
            HttpServletRequest httpRequest,
            @PathVariable UUID deviceId,
            @Valid @RequestBody UpdateDeviceNameRequest request) {

        UUID userId = (UUID) httpRequest.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        deviceCommandService.handle(new UpdateDeviceNameCommand(deviceId, request.name(), new UserId(userId)));
        return ResponseEntity.ok().build();
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

    private DeviceResponse toResponse(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getSerialNumber(),
                device.getName(),
                DeviceStatus.OFFLINE,
                null,
                null,
                Map.of(),
                device.getHardwareId().value(),
                device.getDeviceType().value(),
                null,
                null,
                device.getAuditFields().getCreatedAt() != null ? device.getAuditFields().getCreatedAt().toInstant() : null,
                device.getAuditFields().getUpdatedAt() != null ? device.getAuditFields().getUpdatedAt().toInstant() : null
        );
    }
}
