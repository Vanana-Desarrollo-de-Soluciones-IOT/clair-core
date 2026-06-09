package com.claircore.billing.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User Plan Response")
public record UserPlanResource(
        @Schema(description = "User ID") String userId,
        @Schema(description = "User plan: 'premium' or 'freemium'") String plan,
        @Schema(description = "Active subscription status, or null if freemium") String subscriptionStatus
) {}
