package com.claircore.device.interfaces.rest.resources;

import com.claircore.device.domain.model.valueobjects.PlanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrganizationRequest(
    @NotBlank String name,
    @NotNull PlanType planType
) {}