package com.formai.api.tracking.infrastructure.persistence.repositories;

import com.formai.api.tracking.domain.model.valueobjects.ReportPeriod;
import com.formai.api.tracking.infrastructure.persistence.entities.WorkoutSessionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutSessionJpaRepository extends JpaRepository<WorkoutSessionJpaEntity, UUID> {

    Optional<WorkoutSessionJpaEntity> findByIdAndClientId(UUID id, UUID clientId);

    Optional<WorkoutSessionJpaEntity> findByClientIdAndScheduledFor(UUID clientId, LocalDate date);

    Page<WorkoutSessionJpaEntity> findAllByClientId(UUID clientId, Pageable pagination);

    Page<WorkoutSessionJpaEntity> findAllByClientIdAndScheduledForBetween(UUID clientId, LocalDate from,
                                                                          LocalDate to, Pageable pagination);

    Optional<WorkoutSessionJpaEntity> findFirstByClientIdAndFinishedAtIsNotNullOrderByFinishedAtDesc(UUID clientId);

    List<WorkoutSessionJpaEntity> findAllByStatusAndScheduledForBefore(String status, LocalDate date);

    default Page<WorkoutSessionJpaEntity> findAllByClientId(UUID clientId, Optional<ReportPeriod> period,
                                                           Pageable pagination) {
        return period
                .map(range -> findAllByClientIdAndScheduledForBetween(clientId, range.from(), range.to(), pagination))
                .orElseGet(() -> findAllByClientId(clientId, pagination));
    }

    default Optional<WorkoutSessionJpaEntity> findLastFinishedByClientId(UUID clientId) {
        return findFirstByClientIdAndFinishedAtIsNotNullOrderByFinishedAtDesc(clientId);
    }

    default List<WorkoutSessionJpaEntity> findAllPendingBefore(LocalDate date) {
        return findAllByStatusAndScheduledForBefore("PENDING", date);
    }
}
