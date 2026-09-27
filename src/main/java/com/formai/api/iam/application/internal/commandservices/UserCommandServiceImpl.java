package com.formai.api.iam.application.internal.commandservices;

import com.formai.api.iam.application.internal.outboundservices.hashing.HashingService;
import com.formai.api.iam.domain.exceptions.EmailAlreadyRegisteredException;
import com.formai.api.iam.domain.model.aggregates.User;
import com.formai.api.iam.domain.model.commands.SignInCommand;
import com.formai.api.iam.domain.model.commands.SignUpCommand;
import com.formai.api.iam.domain.model.events.UserRegistered;
import com.formai.api.iam.domain.model.valueobjects.HashedPassword;
import com.formai.api.iam.domain.repositories.UserRepository;
import com.formai.api.iam.domain.services.UserCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final HashingService hashingService;
    private final ApplicationEventPublisher eventPublisher;

    public UserCommandServiceImpl(UserRepository userRepository,
                                   HashingService hashingService,
                                   ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.hashingService = hashingService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<User> handle(SignUpCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyRegisteredException(command.email().value());
        }
        var hashed = new HashedPassword(hashingService.hash(command.rawPassword()));
        var user = User.register(command.email(), hashed);
        var saved = userRepository.save(user);

        eventPublisher.publishEvent(new UserRegistered(saved.getId(), saved.getId().toString()));
        return Optional.of(saved);
    }

    @Override
    public Optional<User> handle(SignInCommand command) {
        return userRepository.findByEmail(command.email())
                .filter(user -> hashingService.matches(command.rawPassword(), user.getHashedPassword().value()));
    }
}
