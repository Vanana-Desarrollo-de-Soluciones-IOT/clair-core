package com.claircore.iam.domain.model.commands;

import com.claircore.iam.domain.model.valueobjects.TokenJti;

public record RotateRefreshTokenCommand(
    TokenJti refreshTokenJti
) {
    public RotateRefreshTokenCommand {
        if (refreshTokenJti == null) throw new IllegalArgumentException("Refresh token JTI is required");
    }
}
