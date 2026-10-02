package com.formai.api.iam.application.internal.commandservices;

import com.formai.api.iam.domain.model.aggregates.RefreshToken;
import com.formai.api.iam.domain.model.commands.IssueRefreshTokenCommand;
import com.formai.api.iam.domain.model.commands.RevokeAllRefreshTokensCommand;
import com.formai.api.iam.domain.model.commands.RevokeRefreshTokenCommand;
import com.formai.api.iam.domain.model.commands.RotateRefreshTokenCommand;
import com.formai.api.iam.domain.model.valueobjects.AccountStatus;
import com.formai.api.iam.domain.model.valueobjects.RefreshedSession;
import com.formai.api.iam.domain.repositories.RefreshTokenRepository;
import com.formai.api.iam.domain.repositories.UserRepository;
import com.formai.api.iam.domain.services.RefreshTokenCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenCommandServiceImpl implements RefreshTokenCommandService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCommandServiceImpl.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final Duration validity;

    public RefreshTokenCommandServiceImpl(RefreshTokenRepository refreshTokenRepository,
                                          UserRepository userRepository,
                                          @Value("${formai.jwt.refresh-expiration-days:7}") long validityDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.validity = Duration.ofDays(validityDays);
    }

    @Override
    @Transactional
    public Optional<RefreshToken> handle(IssueRefreshTokenCommand command) {
        return Optional.of(refreshTokenRepository.save(issue(command)));
    }

    // Rotation: the token used is revoked and a new one takes its place. A token that was already
    // revoked being presented again means it leaked (the legitimate holder rotated it first), so
    // every session of that account is revoked and has to sign in again.
    @Override
    @Transactional
    public Optional<RefreshedSession> handle(RotateRefreshTokenCommand command) {
        var now = Instant.now();
        var found = refreshTokenRepository.findByTokenHash(RefreshToken.hashOf(command.rawToken()));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        var current = found.get();
        if (current.isRevoked()) {
            log.warn("A revoked refresh token was reused; revoking every session of the account");
            revokeAll(current.getUserId(), now);
            return Optional.empty();
        }
        if (!current.isUsableAt(now)) {
            return Optional.empty();
        }
        current.revoke(now);
        refreshTokenRepository.save(current);
        var user = userRepository.findById(current.getUserId())
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE);
        if (user.isEmpty()) {
            return Optional.empty();
        }
        var next = refreshTokenRepository.save(issue(new IssueRefreshTokenCommand(current.getUserId())));
        return Optional.of(new RefreshedSession(user.get(), next.getRawValue()
                .orElseThrow(() -> new IllegalStateException("A just-issued refresh token has its raw value"))));
    }

    @Override
    @Transactional
    public void handle(RevokeRefreshTokenCommand command) {
        refreshTokenRepository.findByTokenHash(RefreshToken.hashOf(command.rawToken())).ifPresent(token -> {
            token.revoke(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public void handle(RevokeAllRefreshTokensCommand command) {
        revokeAll(command.userId(), Instant.now());
    }

    private RefreshToken issue(IssueRefreshTokenCommand command) {
        return RefreshToken.issue(command, Instant.now(), validity);
    }

    private void revokeAll(UUID userId, Instant now) {
        refreshTokenRepository.findAllNotRevokedByUserId(userId).forEach(token -> {
            token.revoke(now);
            refreshTokenRepository.save(token);
        });
    }
}
