package com.claircore.iam.interfaces.rest.transform;

import com.claircore.iam.domain.model.commands.ConfirmRegistrationCommand;
import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;
import com.claircore.iam.interfaces.rest.resources.ConfirmRegistrationRequest;

public class ConfirmRegistrationCommandFromRequestAssembler {
    public static ConfirmRegistrationCommand toCommandFromRequest(ConfirmRegistrationRequest request) {
        return new ConfirmRegistrationCommand(
                new RegistrationSessionId(request.sessionId()),
                request.verificationCode()
        );
    }
}
