package com.claircore.iam.domain.services;

import com.claircore.iam.domain.model.commands.AuthenticateWithGoogleCommand;
import com.claircore.iam.domain.model.entities.User;

import java.util.Optional;

public interface GoogleAuthenticationCommandService {
    Optional<User> handle(AuthenticateWithGoogleCommand command);
}
