package com.formai.api.planning.infrastructure.persistence.repositories;

import com.formai.api.planning.infrastructure.persistence.entities.RoutineJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RoutineJpaRepository extends JpaRepository<RoutineJpaEntity, UUID> {

    Optional<RoutineJpaEntity> findByIdAndHolderId(UUID id, String holderId);

    Page<RoutineJpaEntity> findAllByHolderId(String holderId, Pageable pagination);

    @Query("select case when count(r) > 0 then true else false end from RoutineJpaEntity r join r.versions v "
            + "where r.holderId = :holderId and v.sessionsJson like concat('%', :exerciseId, '%')")
    boolean anyVersionMentions(@Param("holderId") String holderId, @Param("exerciseId") String exerciseId);

    default boolean existsByHolderIdAndExerciseId(String holderId, UUID exerciseId) {
        return anyVersionMentions(holderId, exerciseId.toString());
    }
}
