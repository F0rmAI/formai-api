package com.formai.api.planning.infrastructure.persistence.repositories;

import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.ExerciseName;
import com.formai.api.planning.domain.model.valueobjects.ExerciseOrigin;
import com.formai.api.planning.domain.model.valueobjects.ExercisePage;
import com.formai.api.planning.domain.model.valueobjects.ExerciseStatus;
import com.formai.api.planning.domain.model.valueobjects.MuscleGroup;
import com.formai.api.planning.domain.model.valueobjects.Pagination;
import com.formai.api.planning.domain.repositories.ExerciseRepository;
import com.formai.api.planning.infrastructure.persistence.transform.ExerciseJpaMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ExerciseRepositoryImpl implements ExerciseRepository {

    private static final Sort BY_NAME = Sort.by(Sort.Direction.ASC, "name");

    private final ExerciseJpaRepository jpaRepository;
    private final ExerciseJpaMapper mapper;

    public ExerciseRepositoryImpl(ExerciseJpaRepository jpaRepository, ExerciseJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Exercise save(Exercise exercise) {
        var entity = mapper.toEntity(exercise);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Exercise> findByIdAndHolderId(ExerciseId id, String holderId) {
        return jpaRepository.findByIdAndHolderId(id.value(), holderId).map(mapper::toDomain);
    }

    @Override
    public Optional<Exercise> findAvailableById(ExerciseId id, String holderId) {
        return jpaRepository.findAvailableById(id.value(), holderId).map(mapper::toDomain);
    }

    @Override
    public boolean existsAvailableByName(String holderId, ExerciseName name) {
        return jpaRepository.existsAvailableByName(holderId, name.value());
    }

    @Override
    public ExercisePage findAllAvailableTo(String holderId, Optional<String> search, Optional<MuscleGroup> muscleGroup,
                                           Optional<ExerciseOrigin> origin, Optional<ExerciseStatus> status,
                                           Pagination pagination) {
        var pageRequest = PageRequest.of(pagination.page(), pagination.size(), BY_NAME);
        var page = jpaRepository.findAllAvailableTo(holderId, search, muscleGroup.map(MuscleGroup::value),
                origin.map(Enum::name), status.map(Enum::name), pageRequest);
        return new ExercisePage(page.getContent().stream().map(mapper::toDomain).toList(),
                pagination.page(), pagination.size(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public void delete(Exercise exercise) {
        jpaRepository.deleteById(exercise.getId().value());
    }
}
