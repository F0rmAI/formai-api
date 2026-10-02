package com.formai.api.planning.infrastructure.persistence.repositories;

import com.formai.api.planning.domain.model.aggregates.Routine;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.Pagination;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.RoutinePage;
import com.formai.api.planning.domain.repositories.RoutineRepository;
import com.formai.api.planning.infrastructure.persistence.transform.RoutineJpaMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RoutineRepositoryImpl implements RoutineRepository {

    private static final Sort MOST_RECENT_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final RoutineJpaRepository jpaRepository;
    private final RoutineJpaMapper mapper;

    public RoutineRepositoryImpl(RoutineJpaRepository jpaRepository, RoutineJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Routine save(Routine routine) {
        var entity = mapper.toEntity(routine);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Routine> findByIdAndHolderId(RoutineId id, String holderId) {
        return jpaRepository.findByIdAndHolderId(id.value(), holderId).map(mapper::toDomain);
    }

    @Override
    public Optional<Routine> findById(RoutineId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public RoutinePage findAllByHolderId(String holderId, Pagination pagination) {
        var pageRequest = PageRequest.of(pagination.page(), pagination.size(), MOST_RECENT_FIRST);
        var page = jpaRepository.findAllByHolderId(holderId, pageRequest);
        return new RoutinePage(page.getContent().stream().map(mapper::toDomain).toList(),
                pagination.page(), pagination.size(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public boolean existsByHolderIdAndExerciseId(String holderId, ExerciseId exerciseId) {
        return jpaRepository.existsByHolderIdAndExerciseId(holderId, exerciseId.value());
    }
}
