package com.claircore.iam.infrastructure.tokens.jwt;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

@Component
public class JwtTokenEncoder {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_JTI = "jti";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;

    public JwtTokenEncoder(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(EmailAddress email, long ttlMillis, String jti) {
        return Jwts.builder()
                .subject(email.address())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_JTI, jti)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(EmailAddress email, long ttlMillis, String jti) {
        return Jwts.builder()
                .subject(email.address())
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .claim(CLAIM_JTI, jti)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(secretKey)
                .compact();
    }

    public Optional<String> extractJti(String token) {
        try {
            return Optional.ofNullable(extractClaim(token, claims -> claims.get(CLAIM_JTI, String.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<String> extractEmail(String token) {
        try {
            return Optional.ofNullable(extractClaim(token, Claims::getSubject));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<String> extractType(String token) {
        try {
            return Optional.ofNullable(extractClaim(token, claims -> claims.get(CLAIM_TYPE, String.class)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
