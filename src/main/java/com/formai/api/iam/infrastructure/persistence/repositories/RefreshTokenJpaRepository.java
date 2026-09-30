package com.formai.api.iam.infrastructure.persistence.repositories;

import com.formai.api.iam.infrastructure.persistence.entities.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenJpaEntity> findAllByUserIdAndRevokedAtIsNull(UUID userId);
}
