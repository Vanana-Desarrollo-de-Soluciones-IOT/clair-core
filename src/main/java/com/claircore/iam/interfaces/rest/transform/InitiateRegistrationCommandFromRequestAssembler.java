package com.claircore.iam.interfaces.rest.transform;

import com.claircore.iam.domain.model.commands.InitiateRegistrationCommand;
import com.claircore.iam.interfaces.rest.resources.InitiateRegistrationRequest;

public class InitiateRegistrationCommandFromRequestAssembler {
    public static InitiateRegistrationCommand toCommandFromRequest(InitiateRegistrationRequest request) {
        return new InitiateRegistrationCommand(
                request.email(),
                request.password()
        );
    }
}
