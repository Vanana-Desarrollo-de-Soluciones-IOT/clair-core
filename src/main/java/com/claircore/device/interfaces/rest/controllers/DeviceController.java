package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.commands.DeleteDeviceCommand;
import com.claircore.device.domain.model.commands.PairDeviceCommand;
import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.services.DeviceCommandService;
import com.claircore.device.domain.services.DeviceQueryService;
import com.claircore.device.domain.model.queries.GetDeviceByIdQuery;
import com.claircore.device.domain.model.queries.GetDevicesBySpaceQuery;
import com.claircore.device.interfaces.rest.resources.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Devices", description = "Device pairing and management endpoints")
public class DeviceController {

    private final DeviceCommandService deviceCommandService;
    private final DeviceQueryService deviceQueryService;

    public DeviceController(DeviceCommandService deviceCommandService, DeviceQueryService deviceQueryService) {
        this.deviceCommandService = deviceCommandService;
        this.deviceQueryService = deviceQueryService;
    }

    @PostMapping("/pair")
    @Operation(summary = "Pair a physical device")
    public ResponseEntity<DeviceResponse> pairDevice(@RequestBody PairDeviceRequest request) {
        var command = new PairDeviceCommand(request.hardwareId(), request.deviceType());
        Device device = deviceCommandService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(device));
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Get device by ID")
    public ResponseEntity<DeviceResponse> getDevice(@PathVariable UUID deviceId) {
        var query = new GetDeviceByIdQuery(deviceId);
        return deviceQueryService.handle(query)
            .map(device -> ResponseEntity.ok(toResponse(device)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get devices by space with pagination")
    public ResponseEntity<Page<DeviceResponse>> getDevices(
            @RequestParam UUID spaceId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        var query = new GetDevicesBySpaceQuery(spaceId, page, size);
        Page<Device> devices = deviceQueryService.handle(query);
        return ResponseEntity.ok(devices.map(this::toResponse));
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Delete a device")
    public ResponseEntity<Void> deleteDevice(@PathVariable UUID deviceId) {
        deviceCommandService.handle(new DeleteDeviceCommand(deviceId));
        return ResponseEntity.noContent().build();
    }

    private DeviceResponse toResponse(Device device) {
        return new DeviceResponse(
            device.getId(),
            device.getSerialNumber(),
            device.getName(),
            device.getStatus(),
            device.getSpaceId(),
            device.getConfiguration(),
            device.getHardwareId().value(),
            device.getApiKey().value(),
            device.getDeviceType().value(),
            device.getClaimToken() != null ? device.getClaimToken().value() : null,
            device.getActivatedAt(),
            device.getLastSeenAt(),
            device.getAuditFields().getCreatedAt().toInstant(),
            device.getAuditFields().getUpdatedAt().toInstant()
        );
    }
}
