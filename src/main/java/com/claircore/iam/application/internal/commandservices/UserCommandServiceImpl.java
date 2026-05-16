package com.claircore.iam.application.internal.commandservices;

import com.claircore.iam.application.internal.outboundservices.acl.AsyncNotificationService;
import com.claircore.iam.domain.model.commands.ConfirmRegistrationCommand;
import com.claircore.iam.domain.model.commands.InitiateRegistrationCommand;
import com.claircore.iam.domain.model.entities.RegistrationSession;
import com.claircore.iam.domain.model.entities.User;
import com.claircore.iam.domain.model.valueobjects.*;
import com.claircore.iam.domain.services.UserCommandService;
import com.claircore.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.claircore.iam.infrastructure.persistence.redis.repositories.RegistrationSessionRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.claircore.iam.domain.model.events.UserRegisteredEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final RegistrationSessionRepository registrationSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AsyncNotificationService asyncNotificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom;

    public UserCommandServiceImpl(
            UserRepository userRepository,
            RegistrationSessionRepository registrationSessionRepository,
            PasswordEncoder passwordEncoder,
            AsyncNotificationService asyncNotificationService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.registrationSessionRepository = registrationSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.asyncNotificationService = asyncNotificationService;
        this.eventPublisher = eventPublisher;
        this.secureRandom = new SecureRandom();
    }

    @Override
    @Transactional
    public Optional<RegistrationSession> handle(InitiateRegistrationCommand command) {
        var emailAddress = new EmailAddress(command.email());

        if (userRepository.existsByEmail(emailAddress)) {
            return Optional.empty();
        }

        var sessionId = RegistrationSessionId.generate();
        var verificationCode = generateVerificationCode();
        var passwordHash = passwordEncoder.encode(command.password());

        var session = new RegistrationSession(
                sessionId,
                emailAddress,
                passwordHash,
                verificationCode,
                30
        );

        registrationSessionRepository.save(session);
        asyncNotificationService.sendVerificationCode(emailAddress.address(), verificationCode.code());

        return Optional.of(session);
    }

    @Override
    @Transactional
    public Optional<User> handle(ConfirmRegistrationCommand command) {
        var session = registrationSessionRepository.findById(command.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("Registration session not found or expired"));

        if (session.isExpired()) {
            registrationSessionRepository.deleteById(command.sessionId());
            throw new IllegalArgumentException("Registration session has expired");
        }

        if (!session.verifyCode(command.verificationCode())) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        var emailAddress = session.email();
        if (userRepository.existsByEmail(emailAddress)) {
            throw new IllegalArgumentException("Email already registered");
        }

        var user = new User(
                emailAddress,
                new Password(session.passwordHash())
        );
        user.activate();

        userRepository.save(user);
        eventPublisher.publishEvent(new UserRegisteredEvent(this, user.getId()));
        
        registrationSessionRepository.deleteById(command.sessionId());
        asyncNotificationService.sendWelcomeEmail(user.getEmail().address());

        return Optional.of(user);
    }

    private VerificationCode generateVerificationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            code.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        code.append('-');
        for (int i = 0; i < 4; i++) {
            code.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return new VerificationCode(code.toString());
    }
}
