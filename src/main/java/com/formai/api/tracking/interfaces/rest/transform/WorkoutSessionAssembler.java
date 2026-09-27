package com.formai.api.tracking.interfaces.rest.transform;

import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.commands.CorrectSetCommand;
import com.formai.api.tracking.domain.model.commands.FinishWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.RecordSetCommand;
import com.formai.api.tracking.domain.model.entities.SessionExercise;
import com.formai.api.tracking.domain.model.entities.SetEntry;
import com.formai.api.tracking.domain.model.queries.GetWorkoutHistoryQuery;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseId;
import com.formai.api.tracking.domain.model.valueobjects.Load;
import com.formai.api.tracking.domain.model.valueobjects.Pagination;
import com.formai.api.tracking.domain.model.valueobjects.ReportPeriod;
import com.formai.api.tracking.domain.model.valueobjects.Reps;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionId;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionPage;
import com.formai.api.tracking.interfaces.rest.resources.CreateSessionCompletionResource;
import com.formai.api.tracking.interfaces.rest.resources.CreateSetCorrectionResource;
import com.formai.api.tracking.interfaces.rest.resources.CreateSetResource;
import com.formai.api.tracking.interfaces.rest.resources.SessionExerciseResource;
import com.formai.api.tracking.interfaces.rest.resources.SetEntryResource;
import com.formai.api.tracking.interfaces.rest.resources.WorkoutSessionPageResource;
import com.formai.api.tracking.interfaces.rest.resources.WorkoutSessionResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface WorkoutSessionAssembler {

    @Mapping(target = "totalVolumeKg", expression = "java(session.volume().kilograms())")
    WorkoutSessionResource toResource(WorkoutSession session);

    @Mapping(target = "exerciseName", source = "toPerform.exerciseName")
    @Mapping(target = "targetSets", source = "toPerform.sets")
    @Mapping(target = "targetReps", source = "toPerform.reps")
    @Mapping(target = "targetLoadKg", source = "toPerform.targetLoadKg")
    SessionExerciseResource toResource(SessionExercise exercise);

    @Mapping(target = "loadKg", source = "load.kilograms")
    @Mapping(target = "reps", source = "reps.value")
    SetEntryResource toResource(SetEntry set);

    @Mapping(target = "content", source = "items")
    WorkoutSessionPageResource toResource(WorkoutSessionPage page);

    default RecordSetCommand toCommand(UUID workoutSessionId, String holderId, CreateSetResource resource) {
        return new RecordSetCommand(new WorkoutSessionId(workoutSessionId), toClientId(holderId),
                new ExerciseId(resource.exerciseId()), resource.setNumber(),
                new Load(resource.loadKg()), new Reps(resource.reps()));
    }

    default CorrectSetCommand toCommand(UUID workoutSessionId, String holderId, CreateSetCorrectionResource resource) {
        return new CorrectSetCommand(new WorkoutSessionId(workoutSessionId), toClientId(holderId),
                new ExerciseId(resource.exerciseId()), resource.setNumber(),
                new Load(resource.loadKg()), new Reps(resource.reps()));
    }

    default FinishWorkoutSessionCommand toCommand(UUID workoutSessionId, String holderId,
                                                  CreateSessionCompletionResource resource) {
        return new FinishWorkoutSessionCommand(new WorkoutSessionId(workoutSessionId), toClientId(holderId),
                resource.confirmPartial());
    }

    default GetWorkoutHistoryQuery toHistoryQuery(ClientId clientId, String requesterHolderId,
                                                  LocalDate from, LocalDate to, int page, int size) {
        return new GetWorkoutHistoryQuery(clientId, requesterHolderId, toPeriod(from, to), new Pagination(page, size));
    }

    // holderId is the JWT subject (Authentication.getName()): for a client it is the id of
    // their iam account, which is also their ClientId.
    default ClientId toClientId(String holderId) {
        return new ClientId(UUID.fromString(holderId));
    }

    // The date filter is a closed range: both dates or neither.
    default Optional<ReportPeriod> toPeriod(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return Optional.empty();
        }
        if (from == null || to == null || from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Filter by date with both 'from' and 'to', and 'from' on or before 'to'");
        }
        return Optional.of(new ReportPeriod(from, to));
    }

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(WorkoutSessionId id) {
        return id == null ? null : id.value();
    }

    default UUID map(ExerciseId exerciseId) {
        return exerciseId == null ? null : exerciseId.value();
    }
}
