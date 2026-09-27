package com.formai.api.tracking.infrastructure.persistence.repositories;

import com.formai.api.tracking.infrastructure.persistence.entities.ActiveRoutineJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActiveRoutineJpaRepository extends JpaRepository<ActiveRoutineJpaEntity, UUID> {

    Optional<ActiveRoutineJpaEntity> findByClientId(UUID clientId);

    List<ActiveRoutineJpaEntity> findAllByRoutineId(UUID routineId);

    @Query("select r from ActiveRoutineJpaEntity r "
            + "where r.startDate <= :date and (r.endDate is null or r.endDate >= :date)")
    List<ActiveRoutineJpaEntity> findAllActiveOn(@Param("date") LocalDate date);
}
