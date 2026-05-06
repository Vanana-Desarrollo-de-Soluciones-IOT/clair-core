package com.claircore.iam.interfaces.rest.transform;

import com.claircore.iam.domain.model.entities.RegistrationSession;
import com.claircore.iam.interfaces.rest.resources.RegistrationInitiatedResource;

public class RegistrationInitiatedResourceFromSessionAssembler {
    public static RegistrationInitiatedResource toResourceFromSession(RegistrationSession session) {
        return new RegistrationInitiatedResource(
                session.sessionId().id(),
                "Registration initiated. Please check your email for the verification code."
        );
    }
}
