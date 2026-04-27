package com.claircore.iam.domain.services;

import com.claircore.iam.domain.model.commands.SignUpCommand;
import com.claircore.iam.domain.model.entities.User;

import java.util.Optional;

public interface UserCommandService {
    Optional<User> handle(SignUpCommand command);
}
