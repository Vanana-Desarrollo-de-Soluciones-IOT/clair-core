package com.claircore.iam.domain.services;

import com.claircore.iam.domain.model.commands.ConfirmRegistrationCommand;
import com.claircore.iam.domain.model.commands.InitiateRegistrationCommand;
import com.claircore.iam.domain.model.entities.RegistrationSession;
import com.claircore.iam.domain.model.entities.User;

import java.util.Optional;

public interface UserCommandService {
    Optional<RegistrationSession> handle(InitiateRegistrationCommand command);
    Optional<User> handle(ConfirmRegistrationCommand command);
}
