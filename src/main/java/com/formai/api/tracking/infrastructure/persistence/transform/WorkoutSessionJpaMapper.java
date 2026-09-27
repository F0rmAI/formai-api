package com.formai.api.tracking.infrastructure.persistence.transform;

import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.entities.SessionExercise;
import com.formai.api.tracking.domain.model.entities.SetEntry;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseId;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseToPerform;
import com.formai.api.tracking.domain.model.valueobjects.Load;
import com.formai.api.tracking.domain.model.valueobjects.Reps;
import com.formai.api.tracking.domain.model.valueobjects.RoutineId;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionId;
import com.formai.api.tracking.infrastructure.persistence.entities.PlannedExerciseEmbeddable;
import com.formai.api.tracking.infrastructure.persistence.entities.SetEntryEmbeddable;
import com.formai.api.tracking.infrastructure.persistence.entities.WorkoutSessionJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// The aggregate nests each exercise's sets; the tables keep the prescribed exercises and
// the recorded sets as two flat collections linked by exerciseId.
@Mapper(componentModel = "spring")
public interface WorkoutSessionJpaMapper {

    @Mapping(target = "plannedExercises", source = "exercises")
    @Mapping(target = "sets", source = "exercises", qualifiedByName = "toSetEmbeddables")
    WorkoutSessionJpaEntity toEntity(WorkoutSession workoutSession);

    @Mapping(target = "exercises", source = "entity", qualifiedByName = "toSessionExercises")
    WorkoutSession toDomain(WorkoutSessionJpaEntity entity);

    default PlannedExerciseEmbeddable toPlannedExercise(SessionExercise exercise) {
        var toPerform = exercise.getToPerform();
        var embeddable = new PlannedExerciseEmbeddable();
        embeddable.setExerciseId(toPerform.exerciseId().value());
        embeddable.setExerciseName(toPerform.exerciseName());
        embeddable.setSets(toPerform.sets());
        embeddable.setReps(toPerform.reps());
        embeddable.setTargetLoadKg(toPerform.targetLoadKg());
        embeddable.setRestSeconds(toPerform.restSeconds());
        return embeddable;
    }

    @Named("toSetEmbeddables")
    default List<SetEntryEmbeddable> toSetEmbeddables(List<SessionExercise> exercises) {
        return exercises.stream()
                .flatMap(exercise -> exercise.getSets().stream()
                        .map(set -> toSetEmbeddable(exercise.getExerciseId(), set)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Named("toSessionExercises")
    default List<SessionExercise> toSessionExercises(WorkoutSessionJpaEntity entity) {
        return entity.getPlannedExercises().stream()
                .map(planned -> new SessionExercise(toExerciseToPerform(planned), entity.getSets().stream()
                        .filter(set -> set.getExerciseId().equals(planned.getExerciseId()))
                        .map(this::toSetEntry)
                        .toList()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private SetEntryEmbeddable toSetEmbeddable(ExerciseId exerciseId, SetEntry set) {
        var embeddable = new SetEntryEmbeddable();
        embeddable.setExerciseId(exerciseId.value());
        embeddable.setSetNumber(set.getSetNumber());
        embeddable.setLoadKg(set.getLoad().kilograms());
        embeddable.setReps(set.getReps().value());
        embeddable.setRecordedAt(set.getRecordedAt());
        return embeddable;
    }

    private ExerciseToPerform toExerciseToPerform(PlannedExerciseEmbeddable planned) {
        return new ExerciseToPerform(new ExerciseId(planned.getExerciseId()), planned.getExerciseName(),
                planned.getSets(), planned.getReps(), planned.getTargetLoadKg(), planned.getRestSeconds());
    }

    private SetEntry toSetEntry(SetEntryEmbeddable set) {
        return new SetEntry(set.getSetNumber(), new Load(set.getLoadKg()), new Reps(set.getReps()),
                set.getRecordedAt());
    }

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(WorkoutSessionId id) {
        return id == null ? null : id.value();
    }

    default WorkoutSessionId mapWorkoutSessionId(UUID value) {
        return value == null ? null : new WorkoutSessionId(value);
    }

    default UUID map(ClientId clientId) {
        return clientId == null ? null : clientId.value();
    }

    default ClientId mapClientId(UUID value) {
        return value == null ? null : new ClientId(value);
    }

    default UUID map(RoutineId routineId) {
        return routineId == null ? null : routineId.value();
    }

    default RoutineId mapRoutineId(UUID value) {
        return value == null ? null : new RoutineId(value);
    }
}
