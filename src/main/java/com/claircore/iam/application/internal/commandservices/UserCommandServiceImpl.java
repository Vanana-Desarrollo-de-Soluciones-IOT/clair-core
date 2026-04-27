package com.claircore.iam.application.internal.commandservices;

import com.claircore.iam.domain.model.commands.SignUpCommand;
import com.claircore.iam.domain.model.entities.User;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.domain.model.valueobjects.Username;
import com.claircore.iam.domain.services.NotificationService;
import com.claircore.iam.domain.services.UserCommandService;
import com.claircore.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public UserCommandServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Optional<User> handle(SignUpCommand command) {
        var emailAddress = new EmailAddress(command.email());
        if (userRepository.existsByEmail(emailAddress)) {
            throw new IllegalArgumentException("Email already exists");
        }

        var user = new User(
                new Username(command.username()),
                emailAddress,
                new Password(passwordEncoder.encode(command.password()))
        );

        userRepository.save(user);
        notificationService.sendSignUpConfirmation(user.getEmail());
        
        return Optional.of(user);
    }
}
