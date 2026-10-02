package com.formai.api.iam.application.internal.commandservices;

import com.formai.api.iam.domain.model.aggregates.RefreshToken;
import com.formai.api.iam.domain.model.aggregates.User;
import com.formai.api.iam.domain.model.commands.IssueRefreshTokenCommand;
import com.formai.api.iam.domain.model.commands.RevokeAllRefreshTokensCommand;
import com.formai.api.iam.domain.model.commands.RevokeRefreshTokenCommand;
import com.formai.api.iam.domain.model.commands.RotateRefreshTokenCommand;
import com.formai.api.iam.domain.model.commands.SignUpCommand;
import com.formai.api.iam.domain.model.valueobjects.Email;
import com.formai.api.iam.domain.model.valueobjects.HashedPassword;
import com.formai.api.iam.domain.repositories.RefreshTokenRepository;
import com.formai.api.iam.domain.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCommandServiceImplTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    UserRepository userRepository;

    RefreshTokenCommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        commandService = new RefreshTokenCommandServiceImpl(refreshTokenRepository, userRepository, 7);
    }

    private static User trainer() {
        return User.registerTrainer(new SignUpCommand(new Email("trainer@formai.com"), "secret123", "Ana Trainer"),
                new HashedPassword("hashed"));
    }

    private static RefreshToken tokenOf(User user, Instant issuedAt) {
        return RefreshToken.issue(new IssueRefreshTokenCommand(user.getId()), issuedAt, Duration.ofDays(7));
    }

    private void savingReturnsTheToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldIssueASevenDayTokenWithItsRawValue() {
        savingReturnsTheToken();
        var user = trainer();

        var issued = commandService.handle(new IssueRefreshTokenCommand(user.getId())).orElseThrow();

        assertThat(issued.getRawValue()).isPresent();
        assertThat(issued.getExpiresAt()).isAfter(Instant.now().plus(Duration.ofDays(6)));
    }

    @Test
    void shouldRevokeTheUsedTokenAndIssueANewOneWhenRotating() {
        // Arrange
        savingReturnsTheToken();
        var user = trainer();
        var current = tokenOf(user, Instant.now());
        var raw = current.getRawValue().orElseThrow();
        when(refreshTokenRepository.findByTokenHash(RefreshToken.hashOf(raw))).thenReturn(Optional.of(current));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // Act
        var session = commandService.handle(new RotateRefreshTokenCommand(raw)).orElseThrow();

        // Assert
        assertThat(current.isRevoked()).isTrue();
        assertThat(session.user()).isSameAs(user);
        assertThat(session.refreshToken()).isNotBlank().isNotEqualTo(raw);
    }

    @Test
    void shouldRejectAnUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(RefreshToken.hashOf("unknown"))).thenReturn(Optional.empty());

        assertThat(commandService.handle(new RotateRefreshTokenCommand("unknown"))).isEmpty();
    }

    @Test
    void shouldRejectAnExpiredToken() {
        var user = trainer();
        var expired = tokenOf(user, Instant.now().minus(Duration.ofDays(8)));
        var raw = expired.getRawValue().orElseThrow();
        when(refreshTokenRepository.findByTokenHash(RefreshToken.hashOf(raw))).thenReturn(Optional.of(expired));

        assertThat(commandService.handle(new RotateRefreshTokenCommand(raw))).isEmpty();
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void shouldRevokeEverySessionWhenARevokedTokenIsReused() {
        // Arrange
        savingReturnsTheToken();
        var user = trainer();
        var reused = tokenOf(user, Instant.now());
        var raw = reused.getRawValue().orElseThrow();
        reused.revoke(Instant.now());
        var otherSession = tokenOf(user, Instant.now());
        when(refreshTokenRepository.findByTokenHash(RefreshToken.hashOf(raw))).thenReturn(Optional.of(reused));
        when(refreshTokenRepository.findAllNotRevokedByUserId(user.getId())).thenReturn(List.of(otherSession));

        // Act & Assert
        assertThat(commandService.handle(new RotateRefreshTokenCommand(raw))).isEmpty();
        assertThat(otherSession.isRevoked()).isTrue();
    }

    @Test
    void shouldRejectTheTokenOfADisabledAccount() {
        savingReturnsTheToken();
        var user = trainer();
        user.disable();
        var current = tokenOf(user, Instant.now());
        var raw = current.getRawValue().orElseThrow();
        when(refreshTokenRepository.findByTokenHash(RefreshToken.hashOf(raw))).thenReturn(Optional.of(current));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(commandService.handle(new RotateRefreshTokenCommand(raw))).isEmpty();
        assertThat(current.isRevoked()).isTrue();
    }

    @Test
    void shouldRevokeTheTokenOnSignOut() {
        savingReturnsTheToken();
        var current = tokenOf(trainer(), Instant.now());
        var raw = current.getRawValue().orElseThrow();
        when(refreshTokenRepository.findByTokenHash(RefreshToken.hashOf(raw))).thenReturn(Optional.of(current));

        commandService.handle(new RevokeRefreshTokenCommand(raw));

        assertThat(current.isRevoked()).isTrue();
    }

    @Test
    void shouldRevokeEveryTokenOfAnAccount() {
        savingReturnsTheToken();
        var user = trainer();
        var web = tokenOf(user, Instant.now());
        var mobile = tokenOf(user, Instant.now());
        when(refreshTokenRepository.findAllNotRevokedByUserId(user.getId())).thenReturn(List.of(web, mobile));

        commandService.handle(new RevokeAllRefreshTokensCommand(user.getId()));

        assertThat(web.isRevoked()).isTrue();
        assertThat(mobile.isRevoked()).isTrue();
    }
}
