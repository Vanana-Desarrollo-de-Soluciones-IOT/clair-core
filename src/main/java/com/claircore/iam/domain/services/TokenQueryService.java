package com.claircore.iam.domain.services;

import com.claircore.iam.domain.model.entities.TokenSession;

import java.util.Optional;
import java.util.UUID;

public interface TokenQueryService {
    boolean isAccessTokenValid(String jwtToken);
    boolean isRefreshTokenValid(String jwtToken);
    Optional<String> getEmailFromToken(String jwtToken);
    Optional<UUID> getUserIdFromToken(String jwtToken);
    Optional<TokenSession> getTokenSession(String jwtToken);
}
