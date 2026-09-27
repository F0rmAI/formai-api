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

    // The stored JSON shape of a version's sessions, kept apart from the domain entities so
    // the domain never depends on Jackson.
    record SessionJson(int order, String label, List<PrescribedExerciseJson> exercises) { }

    record PrescribedExerciseJson(UUID exerciseId, String exerciseName, int sets, int reps,
                                  BigDecimal targetLoadKg, int restSeconds) { }

    RoutineJpaEntity toEntity(Routine routine);

    // duplicate(...) returns a Routine, so MapStruct reads it as a fluent setter: it is not.
    @Mapping(target = "duplicate", ignore = true)
    Routine toDomain(RoutineJpaEntity entity);

    default RoutineVersionEmbeddable toEmbeddable(RoutineVersion version) {
        var embeddable = new RoutineVersionEmbeddable();
        embeddable.setNumber(version.getNumber());
        embeddable.setChangedAt(version.getChangedAt());
        embeddable.setAuthor(version.getAuthor());
        embeddable.setSessionsJson(writeSessions(version.getSessions()));
        return embeddable;
    }

    default RoutineVersion toVersion(RoutineVersionEmbeddable embeddable) {
        return new RoutineVersion(embeddable.getNumber(), embeddable.getChangedAt(), embeddable.getAuthor(),
                readSessions(embeddable.getSessionsJson()));
    }

    private String writeSessions(List<RoutineSession> sessions) {
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
            throw new IllegalStateException("Could not serialize the routine sessions", e);
        }
    }

    private List<RoutineSession> readSessions(String sessionsJson) {
        try {
            return JSON.readValue(sessionsJson, SESSIONS).stream()
                    .map(session -> new RoutineSession(session.order(), session.label(), session.exercises().stream()
                            .map(exercise -> new PrescribedExercise(new ExerciseId(exercise.exerciseId()),
                                    exercise.exerciseName(), new Prescription(exercise.sets(), exercise.reps(),
                                    exercise.targetLoadKg(), exercise.restSeconds())))
                            .toList()))
                    .toList();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read the stored routine sessions", e);
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
