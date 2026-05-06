package com.claircore.iam.domain.services;

import com.claircore.iam.domain.model.commands.SignOutCommand;
import com.claircore.iam.domain.model.entities.User;

import java.util.Optional;

public interface TokenCommandService {
    String createAccessToken(User user);
    String createRefreshToken(User user);
    void invalidateAccessToken(String jwtToken);
    void invalidateRefreshToken(String jwtToken);
    Optional<String> rotateRefreshToken(String refreshTokenJwt);
    void signOut(SignOutCommand command);
}
