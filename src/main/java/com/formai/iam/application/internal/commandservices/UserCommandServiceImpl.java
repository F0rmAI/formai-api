package com.formai.iam.application.internal.commandservices;

import com.formai.iam.application.internal.outboundservices.hashing.HashingService;
import com.formai.iam.domain.exceptions.AccountLockedException;
import com.formai.iam.domain.exceptions.ApplicationNotAllowedException;
import com.formai.iam.domain.exceptions.EmailAlreadyRegisteredException;
import com.formai.iam.domain.exceptions.InvalidActivationCodeException;
import com.formai.iam.domain.exceptions.InvalidPasswordResetTokenException;
import com.formai.iam.domain.model.aggregates.User;
import com.formai.iam.domain.model.commands.ActivateAccountCommand;
import com.formai.iam.domain.model.commands.CreateClientAccountCommand;
import com.formai.iam.domain.model.commands.DisableAccountCommand;
import com.formai.iam.domain.model.commands.ReissueActivationCodeCommand;
import com.formai.iam.domain.model.commands.RequestPasswordResetCommand;
import com.formai.iam.domain.model.commands.ResetPasswordCommand;
import com.formai.iam.domain.model.commands.SignInCommand;
import com.formai.iam.domain.model.commands.SignUpCommand;
import com.formai.iam.domain.model.entities.PasswordResetToken;
import com.formai.iam.domain.model.events.AccountActivated;
import com.formai.iam.domain.model.events.AccountLocked;
import com.formai.iam.domain.model.events.ActivationCodeIssued;
import com.formai.iam.domain.model.events.PasswordResetRequested;
import com.formai.iam.domain.model.events.UserRegistered;
import com.formai.iam.domain.model.valueobjects.AccountStatus;
import com.formai.iam.domain.model.valueobjects.HashedPassword;
import com.formai.iam.domain.repositories.UserRepository;
import com.formai.iam.domain.services.UserCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
        var user = User.registerTrainer(command, hashed);
        var saved = userRepository.save(user);

        eventPublisher.publishEvent(new UserRegistered(saved.getId(), saved.getId().toString(),
                command.fullName(), saved.getEmail().value()));
        return Optional.of(saved);
    }

    @Override
    public Optional<User> handle(SignInCommand command) {
        var now = Instant.now();
        var found = userRepository.findByEmail(command.email());
        if (found.isEmpty()) {
            return Optional.empty();
        }
        var user = found.get();
        if (user.isLocked(now)) {
            throw new AccountLockedException(user.getFailedSignIns().lockedUntil());
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            return Optional.empty();
        }
        if (!hashingService.matches(command.rawPassword(), user.getHashedPassword().value())) {
            user.recordFailedSignIn(now);
            userRepository.save(user);
            if (user.isLocked(now)) {
                eventPublisher.publishEvent(new AccountLocked(user.getId(), user.getFailedSignIns().lockedUntil()));
            }
            return Optional.empty();
        }
        if (!user.canSignInFrom(command.application())) {
            throw new ApplicationNotAllowedException();
        }
        user.recordSuccessfulSignIn();
        return Optional.of(userRepository.save(user));
    }

    @Override
    public Optional<User> handle(CreateClientAccountCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyRegisteredException(command.email().value());
        }
        var user = User.createPendingClient(command, Instant.now());
        var saved = userRepository.save(user);

        eventPublisher.publishEvent(new ActivationCodeIssued(saved.getId(), saved.getActivationCode().getExpiresAt()));
        return Optional.of(saved);
    }

    @Override
    public Optional<User> handle(ReissueActivationCodeCommand command) {
        return userRepository.findById(command.userId())
                .filter(user -> user.getStatus() == AccountStatus.PENDING_ACTIVATION)
                .map(user -> {
                    var activationCode = user.reissueActivationCode(Instant.now());
                    var saved = userRepository.save(user);
                    eventPublisher.publishEvent(new ActivationCodeIssued(saved.getId(), activationCode.getExpiresAt()));
                    return saved;
                });
    }

    @Override
    public Optional<User> handle(ActivateAccountCommand command) {
        var now = Instant.now();
        var user = userRepository.findByActivationCode(command.activationCode())
                .orElseThrow(InvalidActivationCodeException::new);
        var hashed = new HashedPassword(hashingService.hash(command.rawPassword()));
        user.activate(command, hashed, now);
        var saved = userRepository.save(user);

        eventPublisher.publishEvent(new AccountActivated(saved.getId(), now));
        return Optional.of(saved);
    }

    @Override
    public void handle(RequestPasswordResetCommand command) {
        userRepository.findByEmail(command.email())
                .filter(user -> user.getStatus() == AccountStatus.ACTIVE)
                .ifPresent(user -> {
                    var token = user.requestPasswordReset(Instant.now());
                    userRepository.save(user);
                    eventPublisher.publishEvent(new PasswordResetRequested(user.getId(), user.getId().toString(),
                            user.getEmail().value(), token.getRawValue(), token.getExpiresAt()));
                });
    }

    @Override
    public void handle(ResetPasswordCommand command) {
        var now = Instant.now();
        var user = userRepository.findByPasswordResetTokenHash(PasswordResetToken.hashOf(command.token()))
                .orElseThrow(InvalidPasswordResetTokenException::new);
        var hashed = new HashedPassword(hashingService.hash(command.rawPassword()));
        user.resetPassword(command, hashed, now);
        userRepository.save(user);
    }

    @Override
    public void handle(DisableAccountCommand command) {
        userRepository.findById(command.userId()).ifPresent(user -> {
            user.disable();
            userRepository.save(user);
        });
    }
}
