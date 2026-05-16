package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.commands.DeleteDeviceCommand;
import com.claircore.device.domain.model.commands.RegisterDeviceCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceConfigurationCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceNameCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceSerialNumberCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceStatusCommand;
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
@Tag(name = "Devices", description = "Device management endpoints")
public class DeviceController {

    private final DeviceCommandService deviceCommandService;
    private final DeviceQueryService deviceQueryService;

    public DeviceController(DeviceCommandService deviceCommandService, DeviceQueryService deviceQueryService) {
        this.deviceCommandService = deviceCommandService;
        this.deviceQueryService = deviceQueryService;
    }

    @PostMapping
    @Operation(summary = "Register a new device")
    public ResponseEntity<DeviceResponse> registerDevice(@RequestBody RegisterDeviceRequest request) {
        var command = new RegisterDeviceCommand(
            request.serialNumber(),
            request.name(),
            UUID.fromString(request.spaceId())
        );

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

    @PatchMapping("/{deviceId}/status")
    @Operation(summary = "Update device status")
    public ResponseEntity<Void> updateDeviceStatus(
            @PathVariable UUID deviceId,
            @RequestBody UpdateDeviceStatusRequest request) {

        var command = new UpdateDeviceStatusCommand(deviceId, request.status());
        deviceCommandService.handle(command);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{deviceId}/configuration")
    @Operation(summary = "Update device configuration")
    public ResponseEntity<Void> updateDeviceConfiguration(
            @PathVariable UUID deviceId,
            @RequestBody UpdateDeviceConfigurationRequest request) {

        var command = new UpdateDeviceConfigurationCommand(deviceId, request.configuration());
        deviceCommandService.handle(command);
        return ResponseEntity.ok().build();
    }

    @PatchMapping({"/{deviceId}/name", "/{deviceId}"})
    @Operation(summary = "Update device name")
    public ResponseEntity<Void> updateDeviceName(
            @PathVariable UUID deviceId,
            @RequestBody UpdateDeviceNameRequest request) {

        var command = new UpdateDeviceNameCommand(deviceId, request.name());
        deviceCommandService.handle(command);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{deviceId}/serial-number")
    @Operation(summary = "Update device serial number")
    public ResponseEntity<Void> updateDeviceSerialNumber(
            @PathVariable UUID deviceId,
            @RequestBody UpdateDeviceSerialNumberRequest request) {

        var command = new UpdateDeviceSerialNumberCommand(deviceId, request.serialNumber());
        deviceCommandService.handle(command);
        return ResponseEntity.ok().build();
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
            device.getAuditFields().getCreatedAt().toInstant(),
            device.getAuditFields().getUpdatedAt().toInstant()
        );
    }
}
