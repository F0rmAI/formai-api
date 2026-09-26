package com.formai.iam.domain.model.aggregates;

import com.formai.iam.domain.exceptions.ConsentRequiredException;
import com.formai.iam.domain.exceptions.InvalidActivationCodeException;
import com.formai.iam.domain.exceptions.InvalidPasswordResetTokenException;
import com.formai.iam.domain.exceptions.PasswordPolicyViolationException;
import com.formai.iam.domain.model.commands.ActivateAccountCommand;
import com.formai.iam.domain.model.commands.CreateClientAccountCommand;
import com.formai.iam.domain.model.commands.ResetPasswordCommand;
import com.formai.iam.domain.model.commands.SignUpCommand;
import com.formai.iam.domain.model.entities.ActivationCode;
import com.formai.iam.domain.model.entities.PasswordResetToken;
import com.formai.iam.domain.model.valueobjects.AccountStatus;
import com.formai.iam.domain.model.valueobjects.ClientApplication;
import com.formai.iam.domain.model.valueobjects.ConsentAcceptance;
import com.formai.iam.domain.model.valueobjects.Email;
import com.formai.iam.domain.model.valueobjects.FailedSignInAttempts;
import com.formai.iam.domain.model.valueobjects.HashedPassword;
import com.formai.iam.domain.model.valueobjects.Role;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class User {

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;
    public static final int MAX_FAILED_SIGN_INS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    public static final Duration ACTIVATION_CODE_VALIDITY = Duration.ofHours(72);
    public static final Duration PASSWORD_RESET_TOKEN_VALIDITY = Duration.ofMinutes(30);

    private static final String ACTIVATION_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ACTIVATION_CODE_LENGTH = 8;
    private static final int PASSWORD_RESET_TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private UUID id;
    private Email email;
    private HashedPassword hashedPassword;
    private Set<Role> roles;
    private AccountStatus status;
    private FailedSignInAttempts failedSignIns;
    private ActivationCode activationCode;
    private PasswordResetToken passwordResetToken;
    private ConsentAcceptance dataConsent;
    private Instant createdAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public User() {
    }

    private User(Email email, Set<Role> roles, AccountStatus status, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.roles = roles;
        this.status = status;
        this.failedSignIns = new FailedSignInAttempts(0, null);
        this.createdAt = createdAt;
    }

    public static User registerTrainer(SignUpCommand command, HashedPassword hashedPassword) {
        assertPasswordPolicy(command.rawPassword());
        var user = new User(command.email(), EnumSet.of(Role.REGISTERED_USER, Role.TRAINER),
                AccountStatus.ACTIVE, Instant.now());
        user.hashedPassword = hashedPassword;
        return user;
    }

    public static User createPendingClient(CreateClientAccountCommand command, Instant now) {
        var user = new User(command.email(), EnumSet.of(Role.REGISTERED_USER, Role.CLIENT),
                AccountStatus.PENDING_ACTIVATION, now);
        user.activationCode = newActivationCode(now);
        return user;
    }

    public ActivationCode reissueActivationCode(Instant now) {
        if (status != AccountStatus.PENDING_ACTIVATION) {
            throw new IllegalStateException("Only accounts pending activation can get a new activation code");
        }
        this.activationCode = newActivationCode(now);
        return activationCode;
    }

    public void activate(ActivateAccountCommand command, HashedPassword hashedPassword, Instant now) {
        if (status != AccountStatus.PENDING_ACTIVATION
                || activationCode == null
                || !activationCode.getCode().equals(command.activationCode())
                || !activationCode.isUsable(now)) {
            throw new InvalidActivationCodeException();
        }
        if (!command.consentAccepted()) {
            throw new ConsentRequiredException();
        }
        assertPasswordPolicy(command.rawPassword());
        this.hashedPassword = hashedPassword;
        this.status = AccountStatus.ACTIVE;
        this.activationCode.markUsed(now);
        this.dataConsent = new ConsentAcceptance(now);
    }

    public boolean canSignInFrom(ClientApplication application) {
        if (hasRole(Role.CLIENT)) {
            return application == ClientApplication.MOBILE_APP;
        }
        if (hasRole(Role.TRAINER) || hasRole(Role.ADMINISTRATOR)) {
            return application == ClientApplication.WEB_PLATFORM;
        }
        return false;
    }

    public void recordFailedSignIn(Instant now) {
        var lockExpired = failedSignIns.lockedUntil() != null && !now.isBefore(failedSignIns.lockedUntil());
        var count = (lockExpired ? 0 : failedSignIns.count()) + 1;
        var lockedUntil = count >= MAX_FAILED_SIGN_INS ? now.plus(LOCK_DURATION) : null;
        this.failedSignIns = new FailedSignInAttempts(count, lockedUntil);
    }

    public void recordSuccessfulSignIn() {
        this.failedSignIns = new FailedSignInAttempts(0, null);
    }

    public boolean isLocked(Instant now) {
        return failedSignIns.lockedUntil() != null && now.isBefore(failedSignIns.lockedUntil());
    }

    public PasswordResetToken requestPasswordReset(Instant now) {
        var rawValue = randomUrlSafeToken();
        this.passwordResetToken = new PasswordResetToken(rawValue, now.plus(PASSWORD_RESET_TOKEN_VALIDITY));
        return passwordResetToken;
    }

    public void resetPassword(ResetPasswordCommand command, HashedPassword hashedPassword, Instant now) {
        if (passwordResetToken == null
                || !passwordResetToken.getTokenHash().equals(PasswordResetToken.hashOf(command.token()))
                || !passwordResetToken.isUsable(now)) {
            throw new InvalidPasswordResetTokenException();
        }
        assertPasswordPolicy(command.rawPassword());
        this.hashedPassword = hashedPassword;
        this.passwordResetToken.markUsed(now);
    }

    public void disable() {
        this.status = AccountStatus.DISABLED;
    }

    // Additive: grants ADMINISTRATOR without ever removing REGISTERED_USER, so an
    // operator keeps using the app normally while also holding elevated access.
    public void grantAdministratorRole() {
        this.roles.add(Role.ADMINISTRATOR);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    private static void assertPasswordPolicy(String rawPassword) {
        if (rawPassword == null
                || rawPassword.length() < MIN_PASSWORD_LENGTH
                || rawPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new PasswordPolicyViolationException();
        }
    }

    private static ActivationCode newActivationCode(Instant now) {
        var code = new StringBuilder(ACTIVATION_CODE_LENGTH);
        for (int i = 0; i < ACTIVATION_CODE_LENGTH; i++) {
            code.append(ACTIVATION_CODE_ALPHABET.charAt(RANDOM.nextInt(ACTIVATION_CODE_ALPHABET.length())));
        }
        return new ActivationCode(code.toString(), now.plus(ACTIVATION_CODE_VALIDITY));
    }

    private static String randomUrlSafeToken() {
        var bytes = new byte[PASSWORD_RESET_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public UUID getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public HashedPassword getHashedPassword() {
        return hashedPassword;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public FailedSignInAttempts getFailedSignIns() {
        return failedSignIns;
    }

    public ActivationCode getActivationCode() {
        return activationCode;
    }

    public PasswordResetToken getPasswordResetToken() {
        return passwordResetToken;
    }

    public ConsentAcceptance getDataConsent() {
        return dataConsent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public void setHashedPassword(HashedPassword hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public void setFailedSignIns(FailedSignInAttempts failedSignIns) {
        this.failedSignIns = failedSignIns;
    }

    public void setActivationCode(ActivationCode activationCode) {
        this.activationCode = activationCode;
    }

    public void setPasswordResetToken(PasswordResetToken passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public void setDataConsent(ConsentAcceptance dataConsent) {
        this.dataConsent = dataConsent;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
