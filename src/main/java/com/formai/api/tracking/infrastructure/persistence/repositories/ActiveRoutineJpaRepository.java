package com.formai.api.tracking.infrastructure.persistence.repositories;

import com.formai.api.tracking.infrastructure.persistence.entities.ActiveRoutineJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActiveRoutineJpaRepository extends JpaRepository<ActiveRoutineJpaEntity, UUID> {

    Optional<ActiveRoutineJpaEntity> findByClientId(UUID clientId);

    List<ActiveRoutineJpaEntity> findAllByRoutineId(UUID routineId);

    List<ActiveRoutineJpaEntity> findAllByClientIdIn(Collection<UUID> clientIds);

    // Active on at least one date of the range: the caller checks each client's own date.
    @Query("select r from ActiveRoutineJpaEntity r "
            + "where r.startDate <= :latest and (r.endDate is null or r.endDate >= :earliest)")
    List<ActiveRoutineJpaEntity> findAllActiveBetween(@Param("earliest") LocalDate earliest,
                                                      @Param("latest") LocalDate latest);

    default List<ActiveRoutineJpaEntity> findAllByClientIds(List<UUID> clientIds) {
        return findAllByClientIdIn(clientIds);
    }
}
