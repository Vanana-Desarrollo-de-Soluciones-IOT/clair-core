package com.claircore.iam.interfaces.rest.transform;

import com.claircore.iam.interfaces.rest.resources.ConfirmRegistrationRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfirmRegistrationCommandFromRequestAssemblerTest {

    @Test
    void shouldMapRequestToCommandWhenRequestIsValid() {
        String sessionId = UUID.randomUUID().toString();
        ConfirmRegistrationRequest request = new ConfirmRegistrationRequest(sessionId, "6G13-789D");

        var command = ConfirmRegistrationCommandFromRequestAssembler.toCommandFromRequest(request);

        assertEquals(sessionId, command.sessionId().id());
        assertEquals("6G13-789D", command.verificationCode());
    }
}
