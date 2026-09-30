package com.formai.api.iam.infrastructure.persistence.repositories;

import com.formai.api.iam.domain.model.aggregates.RefreshToken;
import com.formai.api.iam.domain.repositories.RefreshTokenRepository;
import com.formai.api.iam.infrastructure.persistence.transform.RefreshTokenJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final RefreshTokenJpaMapper mapper;

    public RefreshTokenRepositoryImpl(RefreshTokenJpaRepository jpaRepository, RefreshTokenJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    // The raw value lives only on the instance that issued the token, so it is carried over to
    // the saved copy the caller gets back; the database only ever sees the hash.
    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        jpaRepository.save(mapper.toEntity(refreshToken));
        return refreshToken;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public List<RefreshToken> findAllNotRevokedByUserId(UUID userId) {
        return jpaRepository.findAllByUserIdAndRevokedAtIsNull(userId).stream().map(mapper::toDomain).toList();
    }
}
