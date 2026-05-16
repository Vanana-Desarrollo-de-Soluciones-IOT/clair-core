package com.claircore.device.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record RegisterDeviceRequest(
    @NotBlank String serialNumber,
    @NotBlank String name,
    @NotBlank String spaceId
) {}