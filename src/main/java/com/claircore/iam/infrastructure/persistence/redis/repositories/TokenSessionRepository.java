package com.claircore.iam.infrastructure.persistence.redis.repositories;

import com.claircore.iam.domain.model.entities.TokenSession;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Repository
public class TokenSessionRepository {

    private static final String KEY_PREFIX = "token:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TokenSessionRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(TokenSession session) {
        try {
            String key = buildKey(session.jti().jti(), session.type());
            String value = objectMapper.writeValueAsString(session);
            long ttlSeconds = Duration.between(Instant.now(), session.expiresAt()).getSeconds();
            if (ttlSeconds <= 0) {
                throw new IllegalArgumentException("Token session TTL must be positive");
            }
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize token session", e);
        }
    }

    public Optional<TokenSession> findByJti(TokenJti jti, TokenType type) {
        String key = buildKey(jti.jti(), type);
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

    public void deleteByJti(TokenJti jti, TokenType type) {
        String key = buildKey(jti.jti(), type);
        redisTemplate.delete(key);
    }

    public boolean existsByJti(TokenJti jti, TokenType type) {
        String key = buildKey(jti.jti(), type);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private String buildKey(String jti, TokenType type) {
        return KEY_PREFIX + type.name().toLowerCase() + ":" + jti;
    }
}
