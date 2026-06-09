package com.claircore.iam.interfaces.rest.transform;

import com.claircore.iam.interfaces.rest.resources.InitiateRegistrationRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InitiateRegistrationCommandFromRequestAssemblerTest {

    @Test
    void shouldMapRequestToCommandWhenRequestIsValid() {
        InitiateRegistrationRequest request = new InitiateRegistrationRequest("user@example.com", "SecurePass123!");

        var command = InitiateRegistrationCommandFromRequestAssembler.toCommandFromRequest(request);

        assertEquals("user@example.com", command.email());
        assertEquals("SecurePass123!", command.password());
    }
}
