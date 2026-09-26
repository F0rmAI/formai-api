package com.formai.iam.domain.model.aggregates;

import com.formai.iam.domain.exceptions.ConsentRequiredException;
import com.formai.iam.domain.exceptions.InvalidActivationCodeException;
import com.formai.iam.domain.exceptions.InvalidPasswordResetTokenException;
import com.formai.iam.domain.exceptions.PasswordPolicyViolationException;
import com.formai.iam.domain.model.commands.ActivateAccountCommand;
import com.formai.iam.domain.model.commands.CreateClientAccountCommand;
import com.formai.iam.domain.model.commands.ResetPasswordCommand;
import com.formai.iam.domain.model.commands.SignUpCommand;
import com.formai.iam.domain.model.entities.PasswordResetToken;
import com.formai.iam.domain.model.valueobjects.AccountStatus;
import com.formai.iam.domain.model.valueobjects.ClientApplication;
import com.formai.iam.domain.model.valueobjects.Email;
import com.formai.iam.domain.model.valueobjects.HashedPassword;
import com.formai.iam.domain.model.valueobjects.Role;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");
    private static final HashedPassword HASHED = new HashedPassword("hashed-password");

    private static User trainer() {
        return User.registerTrainer(new SignUpCommand(new Email("trainer@formai.com"), "secret123", "Ana Trainer"), HASHED);
    }

    private static User pendingClient() {
        return User.createPendingClient(new CreateClientAccountCommand(new Email("client@formai.com")), NOW);
    }

    private static ActivateAccountCommand activation(User client, String rawPassword, boolean consentAccepted) {
        return new ActivateAccountCommand(client.getActivationCode().getCode(), rawPassword, consentAccepted);
    }

    @Test
    void shouldRegisterActiveTrainerWhenSigningUp() {
        var user = trainer();

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail().value()).isEqualTo("trainer@formai.com");
        assertThat(user.getHashedPassword()).isEqualTo(HASHED);
        assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.REGISTERED_USER, Role.TRAINER);
        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.getFailedSignIns().count()).isZero();
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldRejectSignUpWhenPasswordIsTooShort() {
        var command = new SignUpCommand(new Email("trainer@formai.com"), "short12", "Ana Trainer");

        assertThatThrownBy(() -> User.registerTrainer(command, HASHED))
                .isInstanceOf(PasswordPolicyViolationException.class);
    }

    @Test
    void shouldRejectSignUpWhenPasswordIsTooLong() {
        var command = new SignUpCommand(new Email("trainer@formai.com"), "a".repeat(129), "Ana Trainer");

        assertThatThrownBy(() -> User.registerTrainer(command, HASHED))
                .isInstanceOf(PasswordPolicyViolationException.class);
    }

    @Test
    void shouldCreatePendingClientWithActivationCodeWhenTrainerAddsClient() {
        var client = pendingClient();

        assertThat(client.getRoles()).containsExactlyInAnyOrder(Role.REGISTERED_USER, Role.CLIENT);
        assertThat(client.getStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
        assertThat(client.getHashedPassword()).isNull();
        assertThat(client.getActivationCode().getCode()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}");
        assertThat(client.getActivationCode().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(72)));
        assertThat(client.getActivationCode().getUsedAt()).isNull();
    }

    @Test
    void shouldReplaceActivationCodeWhenReissued() {
        var client = pendingClient();
        var previousCode = client.getActivationCode();

        var reissued = client.reissueActivationCode(NOW.plus(Duration.ofHours(1)));

        assertThat(client.getActivationCode()).isSameAs(reissued).isNotSameAs(previousCode);
        assertThat(reissued.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(73)));
    }

    @Test
    void shouldRejectReissueWhenAccountIsNotPendingActivation() {
        var user = trainer();

        assertThatThrownBy(() -> user.reissueActivationCode(NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldActivateClientWhenCodeIsValidAndConsentAccepted() {
        var client = pendingClient();
        var activatedAt = NOW.plus(Duration.ofHours(1));

        client.activate(activation(client, "secret123", true), HASHED, activatedAt);

        assertThat(client.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(client.getHashedPassword()).isEqualTo(HASHED);
        assertThat(client.getActivationCode().getUsedAt()).isEqualTo(activatedAt);
        assertThat(client.getDataConsent().acceptedAt()).isEqualTo(activatedAt);
    }

    @Test
    void shouldRejectActivationWhenCodeDoesNotMatch() {
        var client = pendingClient();
        var command = new ActivateAccountCommand("WRONGCOD", "secret123", true);

        assertThatThrownBy(() -> client.activate(command, HASHED, NOW))
                .isInstanceOf(InvalidActivationCodeException.class);
    }

    @Test
    void shouldRejectActivationWhenCodeIsExpired() {
        var client = pendingClient();
        var command = activation(client, "secret123", true);

        assertThatThrownBy(() -> client.activate(command, HASHED, NOW.plus(Duration.ofHours(72))))
                .isInstanceOf(InvalidActivationCodeException.class);
    }

    @Test
    void shouldRejectActivationWhenCodeWasAlreadyUsed() {
        var client = pendingClient();
        var command = activation(client, "secret123", true);
        client.activate(command, HASHED, NOW);

        assertThatThrownBy(() -> client.activate(command, HASHED, NOW))
                .isInstanceOf(InvalidActivationCodeException.class);
    }

    @Test
    void shouldRejectActivationWhenConsentIsNotAccepted() {
        var client = pendingClient();
        var command = activation(client, "secret123", false);

        assertThatThrownBy(() -> client.activate(command, HASHED, NOW))
                .isInstanceOf(ConsentRequiredException.class);
        assertThat(client.getStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
    }

    @Test
    void shouldRejectActivationWhenPasswordViolatesPolicy() {
        var client = pendingClient();
        var command = activation(client, "short", true);

        assertThatThrownBy(() -> client.activate(command, HASHED, NOW))
                .isInstanceOf(PasswordPolicyViolationException.class);
        assertThat(client.getStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
    }

    @Test
    void shouldAllowClientOnlyFromMobileApp() {
        var client = pendingClient();

        assertThat(client.canSignInFrom(ClientApplication.MOBILE_APP)).isTrue();
        assertThat(client.canSignInFrom(ClientApplication.WEB_PLATFORM)).isFalse();
    }

    @Test
    void shouldAllowTrainerOnlyFromWebPlatform() {
        var user = trainer();

        assertThat(user.canSignInFrom(ClientApplication.WEB_PLATFORM)).isTrue();
        assertThat(user.canSignInFrom(ClientApplication.MOBILE_APP)).isFalse();
    }

    @Test
    void shouldLockAccountForFifteenMinutesWhenFifthSignInFails() {
        var user = trainer();

        for (int i = 0; i < 4; i++) {
            user.recordFailedSignIn(NOW);
        }
        assertThat(user.isLocked(NOW)).isFalse();

        user.recordFailedSignIn(NOW);

        assertThat(user.isLocked(NOW)).isTrue();
        assertThat(user.getFailedSignIns().lockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(user.isLocked(NOW.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void shouldRestartFailedCountWhenLockHasExpired() {
        var user = trainer();
        for (int i = 0; i < 5; i++) {
            user.recordFailedSignIn(NOW);
        }

        user.recordFailedSignIn(NOW.plus(Duration.ofMinutes(16)));

        assertThat(user.getFailedSignIns().count()).isEqualTo(1);
        assertThat(user.getFailedSignIns().lockedUntil()).isNull();
    }

    @Test
    void shouldResetFailedCountWhenSignInSucceeds() {
        var user = trainer();
        user.recordFailedSignIn(NOW);

        user.recordSuccessfulSignIn();

        assertThat(user.getFailedSignIns().count()).isZero();
    }

    @Test
    void shouldStoreOnlyTokenHashWhenPasswordResetIsRequested() {
        var user = trainer();

        var token = user.requestPasswordReset(NOW);

        assertThat(token.getRawValue()).isNotBlank();
        assertThat(token.getTokenHash()).isEqualTo(PasswordResetToken.hashOf(token.getRawValue()));
        assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
    }

    @Test
    void shouldReplacePasswordWhenResetTokenIsValid() {
        var user = trainer();
        var token = user.requestPasswordReset(NOW);
        var newHash = new HashedPassword("new-hash");

        user.resetPassword(new ResetPasswordCommand(token.getRawValue(), "newSecret123"), newHash, NOW);

        assertThat(user.getHashedPassword()).isEqualTo(newHash);
        assertThat(user.getPasswordResetToken().getUsedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRejectPasswordResetWhenTokenWasAlreadyUsed() {
        var user = trainer();
        var command = new ResetPasswordCommand(user.requestPasswordReset(NOW).getRawValue(), "newSecret123");
        user.resetPassword(command, HASHED, NOW);

        assertThatThrownBy(() -> user.resetPassword(command, HASHED, NOW))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void shouldRejectPasswordResetWhenTokenIsExpired() {
        var user = trainer();
        var command = new ResetPasswordCommand(user.requestPasswordReset(NOW).getRawValue(), "newSecret123");

        assertThatThrownBy(() -> user.resetPassword(command, HASHED, NOW.plus(Duration.ofMinutes(30))))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void shouldRejectPasswordResetWhenTokenDoesNotMatch() {
        var user = trainer();
        user.requestPasswordReset(NOW);

        assertThatThrownBy(() -> user.resetPassword(new ResetPasswordCommand("forged", "newSecret123"), HASHED, NOW))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void shouldRejectPasswordResetWhenNewPasswordViolatesPolicy() {
        var user = trainer();
        var command = new ResetPasswordCommand(user.requestPasswordReset(NOW).getRawValue(), "short");

        assertThatThrownBy(() -> user.resetPassword(command, HASHED, NOW))
                .isInstanceOf(PasswordPolicyViolationException.class);
    }

    @Test
    void shouldDisableAccount() {
        var user = trainer();

        user.disable();

        assertThat(user.getStatus()).isEqualTo(AccountStatus.DISABLED);
    }
}
