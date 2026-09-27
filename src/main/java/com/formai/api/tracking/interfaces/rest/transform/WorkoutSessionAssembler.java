package com.formai.api.tracking.interfaces.rest.transform;

import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.commands.CorrectSetCommand;
import com.formai.api.tracking.domain.model.commands.FinishWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.RecordSetCommand;
import com.formai.api.tracking.domain.model.entities.SessionExercise;
import com.formai.api.tracking.domain.model.entities.SetEntry;
import com.formai.api.tracking.domain.model.queries.GetClientOverviewsQuery;
import com.formai.api.tracking.domain.model.queries.GetExerciseProgressQuery;
import com.formai.api.tracking.domain.model.queries.GetProgressReportQuery;
import com.formai.api.tracking.domain.model.queries.GetWorkoutHistoryQuery;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.ClientOverview;
import com.formai.api.tracking.domain.model.valueobjects.ClientOverviewPage;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseId;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseMetric;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseProgress;
import com.formai.api.tracking.domain.model.valueobjects.Load;
import com.formai.api.tracking.domain.model.valueobjects.Pagination;
import com.formai.api.tracking.domain.model.valueobjects.ProgressPoint;
import com.formai.api.tracking.domain.model.valueobjects.ProgressReport;
import com.formai.api.tracking.domain.model.valueobjects.ProgressWindow;
import com.formai.api.tracking.domain.model.valueobjects.ReportPeriod;
import com.formai.api.tracking.domain.model.valueobjects.Reps;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionId;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionPage;
import com.formai.api.tracking.interfaces.rest.resources.ClientOverviewPageResource;
import com.formai.api.tracking.interfaces.rest.resources.ClientOverviewResource;
import com.formai.api.tracking.interfaces.rest.resources.CreateSessionCompletionResource;
import com.formai.api.tracking.interfaces.rest.resources.CreateSetCorrectionResource;
import com.formai.api.tracking.interfaces.rest.resources.CreateSetResource;
import com.formai.api.tracking.interfaces.rest.resources.ExerciseMetricResource;
import com.formai.api.tracking.interfaces.rest.resources.ProgressChartResource;
import com.formai.api.tracking.interfaces.rest.resources.ProgressPointResource;
import com.formai.api.tracking.interfaces.rest.resources.ProgressReportResource;
import com.formai.api.tracking.interfaces.rest.resources.SessionExerciseResource;
import com.formai.api.tracking.interfaces.rest.resources.SetEntryResource;
import com.formai.api.tracking.interfaces.rest.resources.WorkoutSessionPageResource;
import com.formai.api.tracking.interfaces.rest.resources.WorkoutSessionResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface WorkoutSessionAssembler {

    @Mapping(target = "totalVolumeKg", source = ".", qualifiedByName = "totalVolumeKg")
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

    @Mapping(target = "from", source = "period.from")
    @Mapping(target = "to", source = "period.to")
    @Mapping(target = "adherencePercentage", source = "adherence.percentage")
    ProgressReportResource toResource(ProgressReport report);

    @Mapping(target = "firstMaxLoadKg", source = "firstMaxLoad.kilograms")
    @Mapping(target = "lastMaxLoadKg", source = "lastMaxLoad.kilograms")
    @Mapping(target = "firstVolumeKg", source = "firstVolume.kilograms")
    @Mapping(target = "lastVolumeKg", source = "lastVolume.kilograms")
    ExerciseMetricResource toResource(ExerciseMetric metric);

    @Mapping(target = "weeks", source = "window")
    ProgressChartResource toResource(ExerciseProgress progress);

    @Mapping(target = "maxLoadKg", source = "maxLoad.kilograms")
    @Mapping(target = "volumeKg", source = "volume.kilograms")
    ProgressPointResource toResource(ProgressPoint point);

    @Mapping(target = "content", source = "items")
    ClientOverviewPageResource toResource(ClientOverviewPage page);

    ClientOverviewResource toResource(ClientOverview overview);

    @Mapping(target = "workoutSessionId", source = "workoutSessionId")
    @Mapping(target = "clientId", source = "holderId")
    @Mapping(target = "exerciseId", source = "resource.exerciseId")
    @Mapping(target = "setNumber", source = "resource.setNumber")
    @Mapping(target = "load", source = "resource.loadKg")
    @Mapping(target = "reps", source = "resource.reps")
    RecordSetCommand toCommand(UUID workoutSessionId, String holderId, CreateSetResource resource);

    @Mapping(target = "workoutSessionId", source = "workoutSessionId")
    @Mapping(target = "clientId", source = "holderId")
    @Mapping(target = "exerciseId", source = "resource.exerciseId")
    @Mapping(target = "setNumber", source = "resource.setNumber")
    @Mapping(target = "load", source = "resource.loadKg")
    @Mapping(target = "reps", source = "resource.reps")
    CorrectSetCommand toCommand(UUID workoutSessionId, String holderId, CreateSetCorrectionResource resource);

    @Mapping(target = "workoutSessionId", source = "workoutSessionId")
    @Mapping(target = "clientId", source = "holderId")
    @Mapping(target = "confirmPartial", source = "resource.confirmPartial")
    FinishWorkoutSessionCommand toCommand(UUID workoutSessionId, String holderId, CreateSessionCompletionResource resource);

    default GetWorkoutHistoryQuery toHistoryQuery(ClientId clientId, String requesterHolderId,
                                                  LocalDate from, LocalDate to, int page, int size) {
        return new GetWorkoutHistoryQuery(clientId, requesterHolderId, toPeriod(from, to), new Pagination(page, size));
    }

    default GetProgressReportQuery toProgressReportQuery(ClientId clientId, String requesterHolderId,
                                                         LocalDate from, LocalDate to) {
        var period = toPeriod(from, to).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A progress report needs both 'from' and 'to'"));
        return new GetProgressReportQuery(clientId, requesterHolderId, period);
    }

    default GetExerciseProgressQuery toExerciseProgressQuery(ClientId clientId, String requesterHolderId,
                                                             UUID exerciseId, int weeks) {
        var window = Arrays.stream(ProgressWindow.values())
                .filter(value -> value.weeks() == weeks)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "weeks must be 4, 8 or 12"));
        return new GetExerciseProgressQuery(clientId, requesterHolderId, new ExerciseId(exerciseId), window);
    }

    default GetClientOverviewsQuery toOverviewsQuery(String holderId, String search, String status, int page,
                                                     int size) {
        return new GetClientOverviewsQuery(holderId, Optional.ofNullable(search).filter(value -> !value.isBlank()),
                Optional.ofNullable(status).filter(value -> !value.isBlank()), new Pagination(page, size));
    }

    default ClientId toClientId(String holderId) {
        return new ClientId(UUID.fromString(holderId));
    }

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

    @Named("totalVolumeKg")
    default BigDecimal totalVolumeKg(WorkoutSession session) {
        return session.volume().kilograms();
    }

    default int toWeeks(ProgressWindow window) {
        return window.weeks();
    }

    default String unwrapText(Optional<String> value) {
        return value.orElse(null);
    }

    default LocalDate unwrapDate(Optional<LocalDate> value) {
        return value.orElse(null);
    }

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(WorkoutSessionId id) {
        return id == null ? null : id.value();
    }

    default WorkoutSessionId mapWorkoutSessionId(UUID value) {
        return value == null ? null : new WorkoutSessionId(value);
    }

    default UUID map(ExerciseId exerciseId) {
        return exerciseId == null ? null : exerciseId.value();
    }

    default ExerciseId mapExerciseId(UUID value) {
        return value == null ? null : new ExerciseId(value);
    }

    default UUID map(ClientId clientId) {
        return clientId == null ? null : clientId.value();
    }

    default Load mapLoad(BigDecimal kilograms) {
        return kilograms == null ? null : new Load(kilograms);
    }

    default Reps mapReps(Integer value) {
        return value == null ? null : new Reps(value);
    }
}
