package com.formai.api.planning.infrastructure.persistence.repositories;

import com.formai.api.planning.infrastructure.persistence.entities.ClientPlanJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ClientPlanJpaRepository extends JpaRepository<ClientPlanJpaEntity, UUID> {

    Optional<ClientPlanJpaEntity> findByClientIdAndHolderId(UUID clientId, String holderId);

    Optional<ClientPlanJpaEntity> findByClientId(UUID clientId);

    @Query("select count(p) > 0 from ClientPlanJpaEntity p join p.assignments a "
            + "where a.routineId = :routineId and a.endDate is null")
    boolean existsOpenAssignmentByRoutineId(@Param("routineId") UUID routineId);
}
