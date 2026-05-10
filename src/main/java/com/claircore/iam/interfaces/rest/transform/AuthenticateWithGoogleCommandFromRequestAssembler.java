package com.claircore.iam.interfaces.rest.transform;

import com.claircore.iam.domain.model.commands.AuthenticateWithGoogleCommand;
import com.claircore.iam.domain.model.valueobjects.GoogleIdToken;
import com.claircore.iam.interfaces.rest.resources.GoogleSignInRequest;

public class AuthenticateWithGoogleCommandFromRequestAssembler {

    private AuthenticateWithGoogleCommandFromRequestAssembler() {
    }

    public static AuthenticateWithGoogleCommand toCommandFromRequest(GoogleSignInRequest request) {
        return new AuthenticateWithGoogleCommand(new GoogleIdToken(request.idToken()));
    }
}
