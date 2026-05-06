package com.claircore.iam.application.internal.commandservices;

import com.claircore.iam.domain.model.commands.CreateTokenSessionCommand;
import com.claircore.iam.domain.model.commands.RotateRefreshTokenCommand;
import com.claircore.iam.domain.model.entities.TokenSession;
import com.claircore.iam.domain.model.entities.User;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import com.claircore.iam.domain.services.TokenCommandService;
import com.claircore.iam.infrastructure.persistence.redis.repositories.TokenSessionRepository;
import com.claircore.iam.infrastructure.tokens.jwt.JwtTokenEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class TokenCommandServiceImpl implements TokenCommandService {

    private final JwtTokenEncoder jwtTokenEncoder;
    private final TokenSessionRepository tokenSessionRepository;
    private final long accessTtlMillis;
    private final long refreshTtlMillis;

    public TokenCommandServiceImpl(
            JwtTokenEncoder jwtTokenEncoder,
            TokenSessionRepository tokenSessionRepository,
            @Value("${jwt.expiration}") long accessTtlMillis,
            @Value("${jwt.refresh-expiration}") long refreshTtlMillis
    ) {
        this.jwtTokenEncoder = jwtTokenEncoder;
        this.tokenSessionRepository = tokenSessionRepository;
        this.accessTtlMillis = accessTtlMillis;
        this.refreshTtlMillis = refreshTtlMillis;
    }

    @Override
    @Transactional
    public String createAccessToken(User user) {
        TokenJti jti = TokenJti.generate();
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(accessTtlMillis);

        TokenSession session = new TokenSession(jti, user.getEmail(), TokenType.ACCESS, now, expiresAt);
        tokenSessionRepository.save(session);

        return jwtTokenEncoder.generateToken(user.getEmail(), accessTtlMillis, jti.jti());
    }

    @Override
    @Transactional
    public String createRefreshToken(User user) {
        TokenJti jti = TokenJti.generate();
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(refreshTtlMillis);

        TokenSession session = new TokenSession(jti, user.getEmail(), TokenType.REFRESH, now, expiresAt);
        tokenSessionRepository.save(session);

        return jwtTokenEncoder.generateRefreshToken(user.getEmail(), refreshTtlMillis, jti.jti());
    }

    @Override
    @Transactional
    public void invalidateAccessToken(String jwtToken) {
        jwtTokenEncoder.extractJti(jwtToken)
                .ifPresent(jti -> tokenSessionRepository.deleteByJti(new TokenJti(jti), TokenType.ACCESS));
    }

    @Override
    @Transactional
    public void invalidateRefreshToken(String jwtToken) {
        jwtTokenEncoder.extractJti(jwtToken)
                .ifPresent(jti -> tokenSessionRepository.deleteByJti(new TokenJti(jti), TokenType.REFRESH));
    }

    @Override
    @Transactional
    public Optional<String> rotateRefreshToken(String refreshTokenJwt) {
        Optional<String> jtiOpt = jwtTokenEncoder.extractJti(refreshTokenJwt);
        Optional<String> typeOpt = jwtTokenEncoder.extractType(refreshTokenJwt);

        if (jtiOpt.isEmpty() || typeOpt.isEmpty() || !"refresh".equals(typeOpt.get())) {
            return Optional.empty();
        }

        TokenJti oldJti = new TokenJti(jtiOpt.get());
        Optional<TokenSession> existingSession = tokenSessionRepository.findByJti(oldJti, TokenType.REFRESH);
        if (existingSession.isEmpty()) {
            return Optional.empty();
        }

        tokenSessionRepository.deleteByJti(oldJti, TokenType.REFRESH);

        TokenJti newJti = TokenJti.generate();
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(refreshTtlMillis);
        TokenSession newSession = new TokenSession(newJti, existingSession.get().email(), TokenType.REFRESH, now, expiresAt);
        tokenSessionRepository.save(newSession);

        return Optional.of(jwtTokenEncoder.generateRefreshToken(existingSession.get().email(), refreshTtlMillis, newJti.jti()));
    }
}
