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
import com.formai.iam.domain.model.valueobjects.ClientApplication;
import com.formai.iam.domain.model.valueobjects.Email;
import com.formai.iam.domain.model.valueobjects.HashedPassword;
import com.formai.iam.domain.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceImplTest {

    private static final Email TRAINER_EMAIL = new Email("trainer@formai.com");
    private static final Email CLIENT_EMAIL = new Email("client@formai.com");

    @Mock
    UserRepository userRepository;

    @Mock
    HashingService hashingService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    UserCommandServiceImpl commandService;

    private static User trainer() {
        return User.registerTrainer(new SignUpCommand(TRAINER_EMAIL, "secret123", "Ana Trainer"),
                new HashedPassword("stored-hash"));
    }

    private static User pendingClient() {
        return User.createPendingClient(new CreateClientAccountCommand(CLIENT_EMAIL), Instant.now());
    }

    private void saveReturnsArgument() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Object publishedEvent() {
        var captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    void shouldThrowWhenSigningUpWithRegisteredEmail() {
        when(userRepository.existsByEmail(TRAINER_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> commandService.handle(new SignUpCommand(TRAINER_EMAIL, "secret123", "Ana Trainer")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRegisterTrainerAndPublishUserRegisteredWhenSigningUp() {
        // Arrange
        when(userRepository.existsByEmail(TRAINER_EMAIL)).thenReturn(false);
        when(hashingService.hash("secret123")).thenReturn("hashed");
        saveReturnsArgument();

        // Act
        var result = commandService.handle(new SignUpCommand(TRAINER_EMAIL, "secret123", "Ana Trainer"));

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getHashedPassword().value()).isEqualTo("hashed");
        var event = (UserRegistered) publishedEvent();
        assertThat(event.userId()).isEqualTo(result.get().getId());
        assertThat(event.holderId()).isEqualTo(result.get().getId().toString());
        assertThat(event.fullName()).isEqualTo("Ana Trainer");
        assertThat(event.email()).isEqualTo("trainer@formai.com");
    }

    @Test
    void shouldReturnEmptyWhenSigningInWithUnknownEmail() {
        when(userRepository.findByEmail(TRAINER_EMAIL)).thenReturn(Optional.empty());

        var result = commandService.handle(new SignInCommand(TRAINER_EMAIL, "secret123", ClientApplication.WEB_PLATFORM));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowWhenSigningInToLockedAccount() {
        var user = trainer();
        for (int i = 0; i < User.MAX_FAILED_SIGN_INS; i++) {
            user.recordFailedSignIn(Instant.now());
        }
        when(userRepository.findByEmail(TRAINER_EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> commandService.handle(
                new SignInCommand(TRAINER_EMAIL, "secret123", ClientApplication.WEB_PLATFORM)))
                .isInstanceOf(AccountLockedException.class);
        verify(hashingService, never()).matches(any(), any());
    }

    @Test
    void shouldReturnEmptyWhenSigningInToAccountPendingActivation() {
        when(userRepository.findByEmail(CLIENT_EMAIL)).thenReturn(Optional.of(pendingClient()));

        var result = commandService.handle(new SignInCommand(CLIENT_EMAIL, "secret123", ClientApplication.MOBILE_APP));

        assertThat(result).isEmpty();
        verify(hashingService, never()).matches(any(), any());
    }

    @Test
    void shouldRecordFailedAttemptWhenPasswordIsWrong() {
        // Arrange
        var user = trainer();
        when(userRepository.findByEmail(TRAINER_EMAIL)).thenReturn(Optional.of(user));
        when(hashingService.matches("wrong-password", "stored-hash")).thenReturn(false);
        saveReturnsArgument();

        // Act
        var result = commandService.handle(new SignInCommand(TRAINER_EMAIL, "wrong-password", ClientApplication.WEB_PLATFORM));

        // Assert
        assertThat(result).isEmpty();
        assertThat(user.getFailedSignIns().count()).isEqualTo(1);
        verify(userRepository).save(user);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void shouldPublishAccountLockedWhenFifthSignInFails() {
        // Arrange
        var user = trainer();
        for (int i = 0; i < User.MAX_FAILED_SIGN_INS - 1; i++) {
            user.recordFailedSignIn(Instant.now());
        }
        when(userRepository.findByEmail(TRAINER_EMAIL)).thenReturn(Optional.of(user));
        when(hashingService.matches("wrong-password", "stored-hash")).thenReturn(false);
        saveReturnsArgument();

        // Act
        commandService.handle(new SignInCommand(TRAINER_EMAIL, "wrong-password", ClientApplication.WEB_PLATFORM));

        // Assert
        var event = (AccountLocked) publishedEvent();
        assertThat(event.userId()).isEqualTo(user.getId());
        assertThat(event.lockedUntil()).isEqualTo(user.getFailedSignIns().lockedUntil());
    }

    @Test
    void shouldThrowWhenSigningInFromNotAllowedApplication() {
        var user = trainer();
        when(userRepository.findByEmail(TRAINER_EMAIL)).thenReturn(Optional.of(user));
        when(hashingService.matches("secret123", "stored-hash")).thenReturn(true);

        assertThatThrownBy(() -> commandService.handle(
                new SignInCommand(TRAINER_EMAIL, "secret123", ClientApplication.MOBILE_APP)))
                .isInstanceOf(ApplicationNotAllowedException.class);
    }

    @Test
    void shouldReturnUserAndResetFailedCountWhenSignInSucceeds() {
        // Arrange
        var user = trainer();
        user.recordFailedSignIn(Instant.now());
        when(userRepository.findByEmail(TRAINER_EMAIL)).thenReturn(Optional.of(user));
        when(hashingService.matches("secret123", "stored-hash")).thenReturn(true);
        saveReturnsArgument();

        // Act
        var result = commandService.handle(new SignInCommand(TRAINER_EMAIL, "secret123", ClientApplication.WEB_PLATFORM));

        // Assert
        assertThat(result).contains(user);
        assertThat(user.getFailedSignIns().count()).isZero();
    }

    @Test
    void shouldThrowWhenCreatingClientWithRegisteredEmail() {
        when(userRepository.existsByEmail(CLIENT_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> commandService.handle(new CreateClientAccountCommand(CLIENT_EMAIL)))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void shouldCreatePendingClientAndPublishActivationCodeIssued() {
        // Arrange
        when(userRepository.existsByEmail(CLIENT_EMAIL)).thenReturn(false);
        saveReturnsArgument();

        // Act
        var result = commandService.handle(new CreateClientAccountCommand(CLIENT_EMAIL));

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
        var event = (ActivationCodeIssued) publishedEvent();
        assertThat(event.userId()).isEqualTo(result.get().getId());
        assertThat(event.expiresAt()).isEqualTo(result.get().getActivationCode().getExpiresAt());
    }

    @Test
    void shouldReturnEmptyWhenReissuingCodeForActiveAccount() {
        var user = trainer();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var result = commandService.handle(new ReissueActivationCodeCommand(user.getId()));

        assertThat(result).isEmpty();
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldIssueNewCodeWhenReissuingForPendingClient() {
        // Arrange
        var client = pendingClient();
        var previousCode = client.getActivationCode();
        when(userRepository.findById(client.getId())).thenReturn(Optional.of(client));
        saveReturnsArgument();

        // Act
        var result = commandService.handle(new ReissueActivationCodeCommand(client.getId()));

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getActivationCode()).isNotSameAs(previousCode);
        assertThat(publishedEvent()).isInstanceOf(ActivationCodeIssued.class);
    }

    @Test
    void shouldThrowWhenActivatingWithUnknownCode() {
        when(userRepository.findByActivationCode("UNKNOWN1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.handle(new ActivateAccountCommand("UNKNOWN1", "secret123", true)))
                .isInstanceOf(InvalidActivationCodeException.class);
    }

    @Test
    void shouldActivateClientAndPublishAccountActivated() {
        // Arrange
        var client = pendingClient();
        var code = client.getActivationCode().getCode();
        when(userRepository.findByActivationCode(code)).thenReturn(Optional.of(client));
        when(hashingService.hash("secret123")).thenReturn("hashed");
        saveReturnsArgument();

        // Act
        var result = commandService.handle(new ActivateAccountCommand(code, "secret123", true));

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(result.get().getHashedPassword().value()).isEqualTo("hashed");
        var event = (AccountActivated) publishedEvent();
        assertThat(event.userId()).isEqualTo(client.getId());
    }

    @Test
    void shouldPublishPasswordResetRequestedWithRawTokenWhenAccountIsActive() {
        // Arrange
        var user = trainer();
        when(userRepository.findByEmail(TRAINER_EMAIL)).thenReturn(Optional.of(user));
        saveReturnsArgument();

        // Act
        commandService.handle(new RequestPasswordResetCommand(TRAINER_EMAIL));

        // Assert
        var event = (PasswordResetRequested) publishedEvent();
        assertThat(event.userId()).isEqualTo(user.getId());
        assertThat(event.email()).isEqualTo("trainer@formai.com");
        assertThat(PasswordResetToken.hashOf(event.token())).isEqualTo(user.getPasswordResetToken().getTokenHash());
        assertThat(event.expiresAt()).isEqualTo(user.getPasswordResetToken().getExpiresAt());
    }

    @Test
    void shouldDoNothingWhenRequestingPasswordResetForUnknownEmail() {
        when(userRepository.findByEmail(TRAINER_EMAIL)).thenReturn(Optional.empty());

        commandService.handle(new RequestPasswordResetCommand(TRAINER_EMAIL));

        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void shouldDoNothingWhenRequestingPasswordResetForPendingClient() {
        when(userRepository.findByEmail(CLIENT_EMAIL)).thenReturn(Optional.of(pendingClient()));

        commandService.handle(new RequestPasswordResetCommand(CLIENT_EMAIL));

        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void shouldThrowWhenResettingPasswordWithUnknownToken() {
        when(userRepository.findByPasswordResetTokenHash(PasswordResetToken.hashOf("forged")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.handle(new ResetPasswordCommand("forged", "newSecret123")))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void shouldReplacePasswordWhenResetTokenIsValid() {
        // Arrange
        var user = trainer();
        var rawToken = user.requestPasswordReset(Instant.now()).getRawValue();
        when(userRepository.findByPasswordResetTokenHash(PasswordResetToken.hashOf(rawToken)))
                .thenReturn(Optional.of(user));
        when(hashingService.hash("newSecret123")).thenReturn("new-hash");
        saveReturnsArgument();

        // Act
        commandService.handle(new ResetPasswordCommand(rawToken, "newSecret123"));

        // Assert
        assertThat(user.getHashedPassword().value()).isEqualTo("new-hash");
        verify(userRepository).save(user);
    }

    @Test
    void shouldDisableAccountWhenUserExists() {
        var user = trainer();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        saveReturnsArgument();

        commandService.handle(new DisableAccountCommand(user.getId()));

        assertThat(user.getStatus()).isEqualTo(AccountStatus.DISABLED);
        verify(userRepository).save(user);
    }
}
