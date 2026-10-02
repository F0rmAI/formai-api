package com.formai.api.iam.domain.repositories;

import com.formai.api.iam.domain.model.aggregates.RefreshToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllNotRevokedByUserId(UUID userId);
}
