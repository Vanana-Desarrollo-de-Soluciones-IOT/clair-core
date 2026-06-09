package com.claircore.iam.infrastructure.persistence.redis.repositories;

import com.claircore.iam.domain.model.entities.RegistrationSession;
import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
public class RegistrationSessionRepository {

    private static final String KEY_PREFIX = "registration:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RegistrationSessionRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public void save(RegistrationSession session) {
        try {
            String key = buildKey(session.sessionId().id());
            String value = objectMapper.writeValueAsString(session);
            long ttlSeconds = Duration.between(session.createdAt(), session.expiresAt()).getSeconds();
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize registration session", e);
        }
    }

    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public Optional<RegistrationSession> findById(RegistrationSessionId sessionId) {
        String key = buildKey(sessionId.id());
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        try {
            RegistrationSession session = objectMapper.readValue(value, RegistrationSession.class);
            return Optional.of(session);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize registration session", e);
        }
    }

    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public void deleteById(RegistrationSessionId sessionId) {
        String key = buildKey(sessionId.id());
        redisTemplate.delete(key);
    }

    private String buildKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
