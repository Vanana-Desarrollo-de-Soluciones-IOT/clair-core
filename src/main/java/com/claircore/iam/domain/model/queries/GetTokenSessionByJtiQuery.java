package com.claircore.iam.domain.model.queries;

import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;

public record GetTokenSessionByJtiQuery(
    TokenJti jti,
    TokenType type
) {
    public GetTokenSessionByJtiQuery {
        if (jti == null) throw new IllegalArgumentException("JTI is required");
        if (type == null) throw new IllegalArgumentException("Token type is required");
    }
}
