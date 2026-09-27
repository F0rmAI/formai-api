package com.formai.api.planning.infrastructure.persistence.transform;

import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.ExerciseName;
import com.formai.api.planning.domain.model.valueobjects.MuscleGroup;
import com.formai.api.planning.infrastructure.persistence.entities.ExerciseJpaEntity;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ExerciseJpaMapper {

    ExerciseJpaEntity toEntity(Exercise exercise);

    Exercise toDomain(ExerciseJpaEntity entity);

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(ExerciseId id) {
        return id == null ? null : id.value();
    }

    default ExerciseId mapExerciseId(UUID value) {
        return value == null ? null : new ExerciseId(value);
    }

    default String map(ExerciseName name) {
        return name == null ? null : name.value();
    }

    default ExerciseName mapExerciseName(String value) {
        return value == null ? null : new ExerciseName(value);
    }

    default String map(MuscleGroup muscleGroup) {
        return muscleGroup == null ? null : muscleGroup.value();
    }

    default MuscleGroup mapMuscleGroup(String value) {
        return value == null ? null : new MuscleGroup(value);
    }
}
