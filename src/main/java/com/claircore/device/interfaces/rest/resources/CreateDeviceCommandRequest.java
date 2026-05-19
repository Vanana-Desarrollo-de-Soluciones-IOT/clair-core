package com.claircore.device.interfaces.rest.resources;

import com.claircore.device.domain.model.valueobjects.DeviceCommandType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to create a command for a device")
public record CreateDeviceCommandRequest(
        @Schema(description = "Command type to execute", example = "STANDBY")
        @NotNull DeviceCommandType type,

        @Schema(description = "Optional command payload as JSON string", example = "{\"reason\":\"user_requested\"}")
        String payload
) {}
