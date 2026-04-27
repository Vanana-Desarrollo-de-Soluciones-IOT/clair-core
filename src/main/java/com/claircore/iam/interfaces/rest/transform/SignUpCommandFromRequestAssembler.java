package com.claircore.iam.interfaces.rest.transform;

import com.claircore.iam.domain.model.commands.SignUpCommand;
import com.claircore.iam.interfaces.rest.resources.SignUpRequest;

public class SignUpCommandFromRequestAssembler {
    public static SignUpCommand toCommandFromRequest(SignUpRequest request) {
        return new SignUpCommand(
                request.email(),
                request.password()
        );
    }
}
