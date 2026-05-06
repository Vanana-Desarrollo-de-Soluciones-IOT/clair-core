package com.claircore.iam.domain.model.commands;

import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;

public record InvalidateTokenSessionCommand(
    TokenJti jti,
    TokenType type
) {
    public InvalidateTokenSessionCommand {
        if (jti == null) throw new IllegalArgumentException("JTI is required");
        if (type == null) throw new IllegalArgumentException("Token type is required");
    }
}
