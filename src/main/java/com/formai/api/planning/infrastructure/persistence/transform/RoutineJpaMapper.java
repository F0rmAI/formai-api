package com.formai.api.planning.infrastructure.persistence.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formai.api.planning.domain.model.aggregates.Routine;
import com.formai.api.planning.domain.model.entities.PrescribedExercise;
import com.formai.api.planning.domain.model.entities.RoutineSession;
import com.formai.api.planning.domain.model.entities.RoutineVersion;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.Prescription;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.RoutineName;
import com.formai.api.planning.infrastructure.persistence.entities.RoutineJpaEntity;
import com.formai.api.planning.infrastructure.persistence.entities.RoutineVersionEmbeddable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface RoutineJpaMapper {

    ObjectMapper JSON = new ObjectMapper();
    TypeReference<List<SessionJson>> SESSIONS = new TypeReference<>() { };

    record SessionJson(int order, String label, List<PrescribedExerciseJson> exercises) { }

    record PrescribedExerciseJson(UUID exerciseId, String exerciseName, int sets, int reps,
                                  BigDecimal targetLoadKg, int restSeconds) { }

    RoutineJpaEntity toEntity(Routine routine);

    // duplicate(...) returns a Routine, so MapStruct reads it as a fluent setter: it is not.
    @Mapping(target = "duplicate", ignore = true)
    Routine toDomain(RoutineJpaEntity entity);

    @Mapping(target = "sessionsJson", source = "sessions")
    RoutineVersionEmbeddable toEmbeddable(RoutineVersion version);

    @Mapping(target = "sessions", source = "sessionsJson")
    RoutineVersion toVersion(RoutineVersionEmbeddable embeddable);

    default String writeSessions(List<RoutineSession> sessions) {
        var json = sessions.stream()
                .map(session -> new SessionJson(session.getOrder(), session.getLabel(), session.getExercises().stream()
                        .map(exercise -> {
                            var prescription = exercise.getPrescription();
                            return new PrescribedExerciseJson(exercise.getExerciseId().value(),
                                    exercise.getExerciseName(), prescription.sets(), prescription.reps(),
                                    prescription.targetLoadKg(), prescription.restSeconds());
                        })
                        .toList()))
                .toList();
        try {
            return JSON.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new RoutineSessionsStorageException("Could not serialize the routine sessions", e);
        }
    }

    default List<RoutineSession> readSessions(String sessionsJson) {
        try {
            return JSON.readValue(sessionsJson, SESSIONS).stream()
                    .map(session -> new RoutineSession(session.order(), session.label(), session.exercises().stream()
                            .map(exercise -> new PrescribedExercise(new ExerciseId(exercise.exerciseId()),
                                    exercise.exerciseName(), new Prescription(exercise.sets(), exercise.reps(),
                                    exercise.targetLoadKg(), exercise.restSeconds())))
                            .toList()))
                    .toList();
        } catch (JsonProcessingException e) {
            throw new RoutineSessionsStorageException("Could not read the stored routine sessions", e);
        }
    }

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(RoutineId id) {
        return id == null ? null : id.value();
    }

    default RoutineId mapRoutineId(UUID value) {
        return value == null ? null : new RoutineId(value);
    }

    default String map(RoutineName name) {
        return name == null ? null : name.value();
    }

    default RoutineName mapRoutineName(String value) {
        return value == null ? null : new RoutineName(value);
    }
}
