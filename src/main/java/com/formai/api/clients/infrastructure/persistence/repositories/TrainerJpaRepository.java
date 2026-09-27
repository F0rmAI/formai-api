package com.formai.api.clients.infrastructure.persistence.repositories;

import com.formai.api.clients.infrastructure.persistence.entities.TrainerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrainerJpaRepository extends JpaRepository<TrainerJpaEntity, UUID> {

    Optional<TrainerJpaEntity> findByHolderId(String holderId);

    boolean existsByHolderId(String holderId);
}
