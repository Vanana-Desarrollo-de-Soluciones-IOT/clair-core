package com.claircore.iam.domain.services;

import com.claircore.iam.domain.model.entities.TokenSession;

import java.util.Optional;

public interface TokenQueryService {
    boolean isAccessTokenValid(String jwtToken);
    boolean isRefreshTokenValid(String jwtToken);
    Optional<String> getEmailFromToken(String jwtToken);
    Optional<TokenSession> getTokenSession(String jwtToken);
}
