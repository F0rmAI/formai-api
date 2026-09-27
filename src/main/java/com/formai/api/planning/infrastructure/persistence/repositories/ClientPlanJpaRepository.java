package com.formai.api.planning.infrastructure.persistence.repositories;

import com.formai.api.planning.infrastructure.persistence.entities.ClientPlanJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientPlanJpaRepository extends JpaRepository<ClientPlanJpaEntity, UUID> {

    Optional<ClientPlanJpaEntity> findByClientIdAndHolderId(UUID clientId, String holderId);

    Optional<ClientPlanJpaEntity> findByClientId(UUID clientId);
}
