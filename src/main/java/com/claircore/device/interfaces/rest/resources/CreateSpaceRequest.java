package com.claircore.device.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record CreateSpaceRequest(
    @NotBlank String name
) {}