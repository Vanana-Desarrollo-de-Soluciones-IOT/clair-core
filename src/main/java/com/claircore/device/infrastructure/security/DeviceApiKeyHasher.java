package com.claircore.device.infrastructure.security;

import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.ApiKeyHash;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Deterministic hashing for device API keys.
 *
 * We store only the hash in the database to avoid leaking device credentials.
 * HMAC-SHA256 keeps lookups fast (indexable) and prevents rainbow-table attacks
 * as long as the pepper stays secret.
 */
@Component
public class DeviceApiKeyHasher {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final byte[] pepperBytes;

    public DeviceApiKeyHasher(@Value("${device.api-key.pepper}") String pepper) {
        if (pepper == null || pepper.isBlank()) {
            throw new IllegalStateException("device.api-key.pepper must be configured");
        }
        this.pepperBytes = pepper.getBytes(StandardCharsets.UTF_8);
    }

    public ApiKeyHash hash(ApiKey apiKey) {
        return hashRaw(apiKey.value());
    }

    public ApiKeyHash hashRaw(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new IllegalArgumentException("API key must not be null or blank");
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepperBytes, "HmacSHA256"));
            byte[] digest = mac.doFinal(rawApiKey.getBytes(StandardCharsets.UTF_8));
            return new ApiKeyHash(ENCODER.encodeToString(digest));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash API key", e);
        }
    }
}
