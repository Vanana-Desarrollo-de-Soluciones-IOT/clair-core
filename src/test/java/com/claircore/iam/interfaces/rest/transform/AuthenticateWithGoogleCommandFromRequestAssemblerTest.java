package com.claircore.iam.interfaces.rest.transform;

import com.claircore.iam.interfaces.rest.resources.GoogleSignInRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthenticateWithGoogleCommandFromRequestAssemblerTest {

    @Test
    void shouldMapGoogleRequestToCommandWhenRequestContainsIdToken() {
        GoogleSignInRequest request = new GoogleSignInRequest("google-id-token");

        var command = AuthenticateWithGoogleCommandFromRequestAssembler.toCommandFromRequest(request);

        assertEquals("google-id-token", command.idToken().token());
    }
}
