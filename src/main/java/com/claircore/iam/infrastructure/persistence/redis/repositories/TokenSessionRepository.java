package com.claircore.iam.infrastructure.persistence.redis.repositories;

import com.claircore.iam.domain.model.entities.TokenSession;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TokenSessionRepository {

    private static final String KEY_PREFIX = "token:";
    private static final String USER_INDEX_PREFIX = "user:tokens:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TokenSessionRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public void save(TokenSession session) {
        try {
            String tokenKey = buildTokenKey(session.jti().jti(), session.type());
            String value = objectMapper.writeValueAsString(session);
            long ttlSeconds = Duration.between(Instant.now(), session.expiresAt()).getSeconds();
            if (ttlSeconds <= 0) {
                throw new IllegalArgumentException("Token session TTL must be positive");
            }
            redisTemplate.opsForValue().set(tokenKey, value, Duration.ofSeconds(ttlSeconds));

            String indexKey = buildUserIndexKey(session.userId(), session.type());
            redisTemplate.opsForValue().set(indexKey, session.jti().jti(), Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize token session", e);
        }
    }

    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public void replaceForUser(TokenSession session) {
        UUID userId = session.userId();
        TokenType type = session.type();

        String indexKey = buildUserIndexKey(userId, type);
        String existingJti = redisTemplate.opsForValue().get(indexKey);
        if (existingJti != null) {
            redisTemplate.delete(buildTokenKey(existingJti, type));
        }

        save(session);
    }

    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public void revokeAllTokensForUser(UUID userId) {
        String accessIndex = buildUserIndexKey(userId, TokenType.ACCESS);
        String refreshIndex = buildUserIndexKey(userId, TokenType.REFRESH);

        String accessJti = redisTemplate.opsForValue().get(accessIndex);
        String refreshJti = redisTemplate.opsForValue().get(refreshIndex);

        if (accessJti != null) {
            redisTemplate.delete(buildTokenKey(accessJti, TokenType.ACCESS));
        }
        if (refreshJti != null) {
            redisTemplate.delete(buildTokenKey(refreshJti, TokenType.REFRESH));
        }

        redisTemplate.delete(accessIndex);
        redisTemplate.delete(refreshIndex);
    }

    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public Optional<TokenSession> findByJti(TokenJti jti, TokenType type) {
        String key = buildTokenKey(jti.jti(), type);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        try {
            TokenSession session = objectMapper.readValue(value, TokenSession.class);
            return Optional.of(session);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize token session", e);
        }
    }

    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public void deleteByJti(TokenJti jti, TokenType type) {
        String key = buildTokenKey(jti.jti(), type);
        redisTemplate.delete(key);
    }

    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public boolean existsByJti(TokenJti jti, TokenType type) {
        String key = buildTokenKey(jti.jti(), type);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private String buildTokenKey(String jti, TokenType type) {
        return KEY_PREFIX + type.name().toLowerCase() + ":" + jti;
    }

    private String buildUserIndexKey(UUID userId, TokenType type) {
        return USER_INDEX_PREFIX + userId + ":" + type.name().toLowerCase();
    }
}
