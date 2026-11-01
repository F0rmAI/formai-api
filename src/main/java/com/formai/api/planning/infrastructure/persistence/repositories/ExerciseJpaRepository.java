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

// A null holderId marks an exercise of the global catalog, available to every trainer.
public interface ExerciseJpaRepository extends JpaRepository<ExerciseJpaEntity, UUID> {

    List<String> ALL_STATUSES = List.of("ACTIVE", "ARCHIVED");

    Optional<ExerciseJpaEntity> findByIdAndHolderId(UUID id, String holderId);

    @Query("select e from ExerciseJpaEntity e where e.id = :id and (e.holderId is null or e.holderId = :holderId)")
    Optional<ExerciseJpaEntity> findAvailableById(@Param("id") UUID id, @Param("holderId") String holderId);

    @Query("select count(e) > 0 from ExerciseJpaEntity e where lower(e.name) = lower(:name) "
            + "and (e.holderId is null or e.holderId = :holderId)")
    boolean existsAvailableByName(@Param("holderId") String holderId, @Param("name") String name);

    // muscleGroup '' means any; includeGlobal / includeOwn select the origin.
    @Query("select e from ExerciseJpaEntity e "
            + "where ((:includeGlobal = true and e.holderId is null) or (:includeOwn = true and e.holderId = :holderId)) "
            + "and lower(e.name) like lower(concat('%', :search, '%')) "
            + "and (:muscleGroup = '' or lower(e.muscleGroup) = lower(:muscleGroup)) "
            + "and e.status in :statuses")
    Page<ExerciseJpaEntity> search(@Param("holderId") String holderId,
                                   @Param("includeGlobal") boolean includeGlobal,
                                   @Param("includeOwn") boolean includeOwn,
                                   @Param("search") String search,
                                   @Param("muscleGroup") String muscleGroup,
                                   @Param("statuses") Collection<String> statuses,
                                   Pageable pagination);

    default Page<ExerciseJpaEntity> findAllAvailableTo(String holderId, Optional<String> search,
                                                      Optional<String> muscleGroup, Optional<String> origin,
                                                      Optional<String> status, Pageable pagination) {
        var includeGlobal = origin.map("GLOBAL"::equals).orElse(true);
        var includeOwn = origin.map("OWN"::equals).orElse(true);
        return search(holderId, includeGlobal, includeOwn, search.orElse(""), muscleGroup.orElse(""),
                status.map(List::of).orElse(ALL_STATUSES), pagination);
    }
}
