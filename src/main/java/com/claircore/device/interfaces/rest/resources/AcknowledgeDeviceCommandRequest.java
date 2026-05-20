package com.claircore.device.interfaces.rest.resources;

import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request used by the edge to acknowledge a device command")
public record AcknowledgeDeviceCommandRequest(
        @Schema(description = "Execution result. Only EXECUTED or FAILED are accepted", example = "EXECUTED")
        @NotNull DeviceCommandStatus status,

        @Schema(description = "Failure reason when status is FAILED", example = "Embedded device did not respond")
        String failureReason
) {}
