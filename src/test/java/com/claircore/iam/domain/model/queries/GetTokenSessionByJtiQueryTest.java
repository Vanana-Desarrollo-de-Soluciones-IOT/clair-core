package com.claircore.iam.domain.model.queries;

import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GetTokenSessionByJtiQueryTest {

    @Test
    void shouldCreateGetTokenSessionByJtiQueryWhenValuesAreValid() {
        TokenJti jti = new TokenJti(UUID.randomUUID().toString());

        GetTokenSessionByJtiQuery query = new GetTokenSessionByJtiQuery(jti, TokenType.REFRESH);

        assertEquals(jti, query.jti());
        assertEquals(TokenType.REFRESH, query.type());
    }

    @Test
    void shouldThrowExceptionWhenJtiIsMissing() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetTokenSessionByJtiQuery(null, TokenType.ACCESS)
        );

        assertEquals("JTI is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenTokenTypeIsMissing() {
        TokenJti jti = new TokenJti(UUID.randomUUID().toString());

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new GetTokenSessionByJtiQuery(jti, null)
        );

        assertEquals("Token type is required", exception.getMessage());
    }
}
