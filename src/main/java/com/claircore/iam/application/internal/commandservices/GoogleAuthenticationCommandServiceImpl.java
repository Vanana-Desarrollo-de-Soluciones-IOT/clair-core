package com.claircore.iam.application.internal.commandservices;

import com.claircore.iam.domain.model.commands.AuthenticateWithGoogleCommand;
import com.claircore.iam.domain.model.entities.User;
import com.claircore.iam.domain.model.events.UserAuthenticatedWithGoogleEvent;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.OAuthProvider;
import com.claircore.iam.domain.services.GoogleAuthenticationCommandService;
import com.claircore.iam.domain.services.GoogleTokenVerifier;
import com.claircore.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GoogleAuthenticationCommandServiceImpl implements GoogleAuthenticationCommandService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public GoogleAuthenticationCommandServiceImpl(
            GoogleTokenVerifier googleTokenVerifier,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Optional<User> handle(AuthenticateWithGoogleCommand command) {
        var verifiedIdentity = googleTokenVerifier.verify(command.idToken());
        if (verifiedIdentity.isEmpty()) {
            return Optional.empty();
        }

        EmailAddress email = verifiedIdentity.get().email();
        String subject = verifiedIdentity.get().userId().subject();

        var existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (!user.isActive()) {
                user.activate();
            }
            if (!user.isOAuthUser()) {
                user.linkOAuthAccount(OAuthProvider.GOOGLE, subject);
            }
        } else {
            user = new User(email, OAuthProvider.GOOGLE, subject);
            userRepository.save(user);
        }

        eventPublisher.publishEvent(new UserAuthenticatedWithGoogleEvent(
                user.getId(),
                user.getEmail(),
                OAuthProvider.GOOGLE
        ));

        return Optional.of(user);
    }
}
