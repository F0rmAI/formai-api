package com.formai.api.planning.infrastructure.persistence.repositories;

import com.formai.api.planning.infrastructure.persistence.entities.ExerciseJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseJpaRepository extends JpaRepository<ExerciseJpaEntity, UUID> {

    List<String> ALL_STATUSES = List.of("ACTIVE", "ARCHIVED");

    Optional<ExerciseJpaEntity> findByIdAndHolderId(UUID id, String holderId);

    boolean existsByHolderIdAndNameIgnoreCase(String holderId, String name);

    @Query("select e from ExerciseJpaEntity e where e.holderId = :holderId "
            + "and lower(e.name) like lower(concat('%', :search, '%')) and e.status in :statuses")
    Page<ExerciseJpaEntity> search(@Param("holderId") String holderId, @Param("search") String search,
                                   @Param("statuses") Collection<String> statuses, Pageable pagination);

    // Duplicates are detected ignoring case: "Squat" and "squat" are the same exercise.
    default boolean existsByHolderIdAndName(String holderId, String name) {
        return existsByHolderIdAndNameIgnoreCase(holderId, name);
    }

    // No search matches every name, and no status matches both.
    default Page<ExerciseJpaEntity> findAllByHolderId(String holderId, Optional<String> search,
                                                     Optional<String> status, Pageable pagination) {
        return search(holderId, search.orElse(""), status.map(List::of).orElse(ALL_STATUSES), pagination);
    }
}
