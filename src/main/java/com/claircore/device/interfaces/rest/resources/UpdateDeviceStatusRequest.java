package com.claircore.device.interfaces.rest.resources;

import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDeviceStatusRequest(@NotNull DeviceStatus status) {}