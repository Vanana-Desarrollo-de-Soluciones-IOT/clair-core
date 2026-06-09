package com.claircore.iam.application.internal.commandservices;

import com.claircore.iam.domain.model.commands.AuthenticateWithGoogleCommand;
import com.claircore.iam.domain.model.entities.User;
import com.claircore.iam.domain.model.valueobjects.GoogleIdToken;
import com.claircore.iam.domain.services.GoogleAuthenticationCommandService;
import com.claircore.iam.infrastructure.oauth.google.GoogleAuthorizationCodeTokenClient;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GoogleOAuthCallbackApplicationService {

    private final GoogleAuthorizationCodeTokenClient tokenClient;
    private final GoogleAuthenticationCommandService authenticationCommandService;

    public GoogleOAuthCallbackApplicationService(
            GoogleAuthorizationCodeTokenClient tokenClient,
            GoogleAuthenticationCommandService authenticationCommandService
    ) {
        this.tokenClient = tokenClient;
        this.authenticationCommandService = authenticationCommandService;
    }

    public Optional<User> handle(String code, String clientId, String clientSecret, String redirectUri) {
        var idTokenOpt = tokenClient.exchangeCodeForIdToken(code, clientId, clientSecret, redirectUri);
        if (idTokenOpt.isEmpty()) {
            return Optional.empty();
        }

        var command = new AuthenticateWithGoogleCommand(new GoogleIdToken(idTokenOpt.get()));
        return authenticationCommandService.handle(command);
    }
}
