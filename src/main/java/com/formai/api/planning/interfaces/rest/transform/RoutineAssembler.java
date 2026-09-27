package com.formai.api.planning.interfaces.rest.transform;

import com.formai.api.planning.domain.model.aggregates.Routine;
import com.formai.api.planning.domain.model.commands.CreateRoutineCommand;
import com.formai.api.planning.domain.model.commands.DuplicateRoutineCommand;
import com.formai.api.planning.domain.model.commands.UpdateRoutineCommand;
import com.formai.api.planning.domain.model.entities.PrescribedExercise;
import com.formai.api.planning.domain.model.entities.RoutineSession;
import com.formai.api.planning.domain.model.entities.RoutineVersion;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.Prescription;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.RoutineName;
import com.formai.api.planning.domain.model.valueobjects.RoutinePage;
import com.formai.api.planning.interfaces.rest.resources.CreateRoutineDuplicateResource;
import com.formai.api.planning.interfaces.rest.resources.CreateRoutineResource;
import com.formai.api.planning.interfaces.rest.resources.CreateRoutineSessionResource;
import com.formai.api.planning.interfaces.rest.resources.PrescribedExerciseResource;
import com.formai.api.planning.interfaces.rest.resources.RoutinePageResource;
import com.formai.api.planning.interfaces.rest.resources.RoutineResource;
import com.formai.api.planning.interfaces.rest.resources.RoutineSessionResource;
import com.formai.api.planning.interfaces.rest.resources.RoutineVersionResource;
import com.formai.api.planning.interfaces.rest.resources.UpdateRoutineResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Mapper(componentModel = "spring")
public interface RoutineAssembler {

    @Mapping(target = "currentVersion", source = ".", qualifiedByName = "currentVersionNumber")
    @Mapping(target = "sessions", source = ".", qualifiedByName = "currentSessions")
    RoutineResource toResource(Routine routine);

    @Mapping(target = "content", source = "items")
    RoutinePageResource toResource(RoutinePage page);

    RoutineVersionResource toResource(RoutineVersion version);

    List<RoutineVersionResource> toVersionResources(List<RoutineVersion> versions);

    List<RoutineSessionResource> toSessionResources(List<RoutineSession> sessions);

    RoutineSessionResource toResource(RoutineSession session);

    @Mapping(target = "sets", source = "prescription.sets")
    @Mapping(target = "reps", source = "prescription.reps")
    @Mapping(target = "targetLoadKg", source = "prescription.targetLoadKg")
    @Mapping(target = "restSeconds", source = "prescription.restSeconds")
    PrescribedExerciseResource toResource(PrescribedExercise exercise);

    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "name", source = "resource.name")
    @Mapping(target = "sessions", source = "resource.sessions", qualifiedByName = "toSessions")
    CreateRoutineCommand toCommand(String holderId, CreateRoutineResource resource);

    @Mapping(target = "routineId", source = "routineId")
    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "name", source = "resource.name")
    @Mapping(target = "sessions", source = "resource.sessions", qualifiedByName = "toSessions")
    UpdateRoutineCommand toCommand(UUID routineId, String holderId, UpdateRoutineResource resource);

    @Mapping(target = "routineId", source = "routineId")
    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "name", source = "resource.name")
    DuplicateRoutineCommand toCommand(UUID routineId, String holderId, CreateRoutineDuplicateResource resource);

    @Named("currentVersionNumber")
    default int currentVersionNumber(Routine routine) {
        return routine.currentVersion().getNumber();
    }

    @Named("currentSessions")
    default List<RoutineSessionResource> currentSessions(Routine routine) {
        return toSessionResources(routine.currentVersion().getSessions());
    }

    @Named("toSessions")
    default List<RoutineSession> toSessions(List<CreateRoutineSessionResource> sessions) {
        return IntStream.range(0, sessions.size())
                .mapToObj(index -> {
                    var session = sessions.get(index);
                    return new RoutineSession(index + 1, session.label(), session.exercises().stream()
                            .map(exercise -> new PrescribedExercise(new ExerciseId(exercise.exerciseId()), null,
                                    new Prescription(exercise.sets(), exercise.reps(), exercise.targetLoadKg(),
                                            exercise.restSeconds())))
                            .toList());
                })
                .toList();
    }

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(RoutineId id) {
        return id == null ? null : id.value();
    }

    default RoutineId mapRoutineId(UUID value) {
        return value == null ? null : new RoutineId(value);
    }

    default UUID map(ExerciseId id) {
        return id == null ? null : id.value();
    }

    default String map(RoutineName name) {
        return name == null ? null : name.value();
    }

    default RoutineName mapRoutineName(String value) {
        return value == null ? null : new RoutineName(value);
    }
}
