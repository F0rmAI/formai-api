package com.formai.api.tracking.infrastructure.persistence.repositories;

import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.LastWorkout;
import com.formai.api.tracking.domain.model.valueobjects.ReportPeriod;
import com.formai.api.tracking.infrastructure.persistence.entities.WorkoutSessionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutSessionJpaRepository extends JpaRepository<WorkoutSessionJpaEntity, UUID> {

    Optional<WorkoutSessionJpaEntity> findByIdAndClientId(UUID id, UUID clientId);

    Optional<WorkoutSessionJpaEntity> findByClientIdAndScheduledFor(UUID clientId, LocalDate date);

    Page<WorkoutSessionJpaEntity> findAllByClientId(UUID clientId, Pageable pagination);

    Page<WorkoutSessionJpaEntity> findAllByClientIdAndScheduledForBetween(UUID clientId, LocalDate from,
                                                                          LocalDate to, Pageable pagination);

    List<WorkoutSessionJpaEntity> findAllByClientIdAndScheduledForBetweenOrderByScheduledForAsc(UUID clientId,
                                                                                              LocalDate from,
                                                                                              LocalDate to);

    @Query("select distinct s from WorkoutSessionJpaEntity s join s.sets e "
            + "where s.clientId = :clientId and e.exerciseId = :exerciseId and s.scheduledFor >= :since")
    List<WorkoutSessionJpaEntity> findAllByClientIdAndExerciseIdSince(@Param("clientId") UUID clientId,
                                                                      @Param("exerciseId") UUID exerciseId,
                                                                      @Param("since") LocalDate since);

    Optional<WorkoutSessionJpaEntity> findFirstByClientIdAndFinishedAtIsNotNullOrderByFinishedAtDesc(UUID clientId);

    List<WorkoutSessionJpaEntity> findAllByStatusAndScheduledForBefore(String status, LocalDate date);

    List<WorkoutSessionJpaEntity> findAllByClientIdAndStatusAndScheduledForBefore(UUID clientId, String status,
                                                                               LocalDate date);

    List<WorkoutSessionJpaEntity> findAllByClientIdAndRoutineIdOrderByScheduledForDesc(UUID clientId, UUID routineId);

    @Query("select s.clientId, max(s.scheduledFor) from WorkoutSessionJpaEntity s "
            + "where s.clientId in :clientIds and s.status in ('COMPLETED', 'PARTIAL') group by s.clientId")
    List<Object[]> findLastTrainedDates(@Param("clientIds") Collection<UUID> clientIds);

    default Page<WorkoutSessionJpaEntity> findAllByClientId(UUID clientId, Optional<ReportPeriod> period,
                                                           Pageable pagination) {
        return period
                .map(range -> findAllByClientIdAndScheduledForBetween(clientId, range.from(), range.to(), pagination))
                .orElseGet(() -> findAllByClientId(clientId, pagination));
    }

    default List<WorkoutSessionJpaEntity> findAllByClientIdAndPeriod(UUID clientId, ReportPeriod period) {
        return findAllByClientIdAndScheduledForBetweenOrderByScheduledForAsc(clientId, period.from(), period.to());
    }

    default Optional<WorkoutSessionJpaEntity> findLastFinishedByClientId(UUID clientId) {
        return findFirstByClientIdAndFinishedAtIsNotNullOrderByFinishedAtDesc(clientId);
    }

    default List<WorkoutSessionJpaEntity> findAllPendingBefore(LocalDate date) {
        return findAllByStatusAndScheduledForBefore("PENDING", date);
    }

    default List<WorkoutSessionJpaEntity> findAllPendingByClientIdBefore(UUID clientId, LocalDate date) {
        return findAllByClientIdAndStatusAndScheduledForBefore(clientId, "PENDING", date);
    }

    default List<LastWorkout> findLastWorkoutDates(List<UUID> clientIds) {
        return findLastTrainedDates(clientIds).stream()
                .map(row -> new LastWorkout(new ClientId((UUID) row[0]), (LocalDate) row[1]))
                .toList();
    }
}
