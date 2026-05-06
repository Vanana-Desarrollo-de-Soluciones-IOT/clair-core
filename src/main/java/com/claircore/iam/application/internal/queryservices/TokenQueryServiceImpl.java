package com.claircore.iam.application.internal.queryservices;

import com.claircore.iam.domain.model.entities.TokenSession;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import com.claircore.iam.domain.services.TokenQueryService;
import com.claircore.iam.infrastructure.persistence.redis.repositories.TokenSessionRepository;
import com.claircore.iam.infrastructure.tokens.jwt.JwtTokenEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TokenQueryServiceImpl implements TokenQueryService {

    private final TokenSessionRepository tokenSessionRepository;
    private final JwtTokenEncoder jwtTokenEncoder;

    public TokenQueryServiceImpl(TokenSessionRepository tokenSessionRepository, JwtTokenEncoder jwtTokenEncoder) {
        this.tokenSessionRepository = tokenSessionRepository;
        this.jwtTokenEncoder = jwtTokenEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAccessTokenValid(String jwtToken) {
        return isTokenValidByType(jwtToken, TokenType.ACCESS);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRefreshTokenValid(String jwtToken) {
        return isTokenValidByType(jwtToken, TokenType.REFRESH);
    }

    private boolean isTokenValidByType(String jwtToken, TokenType expectedType) {
        Optional<String> jti = jwtTokenEncoder.extractJti(jwtToken);
        Optional<String> type = jwtTokenEncoder.extractType(jwtToken);

        if (jti.isEmpty() || type.isEmpty()) {
            return false;
        }

        if (!expectedType.name().toLowerCase().equals(type.get())) {
            return false;
        }

        return tokenSessionRepository.existsByJti(new TokenJti(jti.get()), expectedType);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getEmailFromToken(String jwtToken) {
        return jwtTokenEncoder.extractEmail(jwtToken);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TokenSession> getTokenSession(String jwtToken) {
        Optional<String> jti = jwtTokenEncoder.extractJti(jwtToken);
        Optional<String> type = jwtTokenEncoder.extractType(jwtToken);

        if (jti.isEmpty() || type.isEmpty()) {
            return Optional.empty();
        }

        TokenType tokenType = switch (type.get()) {
            case "access" -> TokenType.ACCESS;
            case "refresh" -> TokenType.REFRESH;
            default -> null;
        };

        if (tokenType == null) {
            return Optional.empty();
        }

        return tokenSessionRepository.findByJti(new TokenJti(jti.get()), tokenType);
    }
}
