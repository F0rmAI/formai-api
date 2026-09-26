package com.formai.iam.infrastructure.persistence.repositories;

import com.formai.iam.infrastructure.persistence.entities.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select u from UserJpaEntity u where u.activationCode.code = :code")
    Optional<UserJpaEntity> findByActivationCode(@Param("code") String code);

    @Query("select u from UserJpaEntity u where u.passwordResetToken.tokenHash = :tokenHash")
    Optional<UserJpaEntity> findByPasswordResetTokenHash(@Param("tokenHash") String tokenHash);
}
