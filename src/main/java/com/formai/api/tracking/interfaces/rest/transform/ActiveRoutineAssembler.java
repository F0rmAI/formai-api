package com.formai.api.tracking.interfaces.rest.transform;

import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseId;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseToPerform;
import com.formai.api.tracking.domain.model.valueobjects.RoutineDay;
import com.formai.api.tracking.domain.model.valueobjects.RoutineId;
import com.formai.api.tracking.domain.model.valueobjects.TodayPlan;
import com.formai.api.tracking.interfaces.rest.resources.ActiveRoutineResource;
import com.formai.api.tracking.interfaces.rest.resources.ExerciseToPerformResource;
import com.formai.api.tracking.interfaces.rest.resources.RoutineDayResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ActiveRoutineAssembler {

    @Mapping(target = "routineId", source = "routine.routineId")
    @Mapping(target = "routineName", source = "routine.routineName")
    @Mapping(target = "version", source = "routine.version")
    @Mapping(target = "startDate", source = "routine.startDate")
    @Mapping(target = "trainingDays", source = "routine.trainingDays")
    @Mapping(target = "sessions", source = "routine.days")
    @Mapping(target = "todaySessionOrder", source = "todaySession", qualifiedByName = "todaySessionOrder")
    @Mapping(target = "todayWorkoutSessionId", source = "todaySession", qualifiedByName = "todayWorkoutSessionId")
    ActiveRoutineResource toResource(TodayPlan plan);

    RoutineDayResource toResource(RoutineDay day);

    ExerciseToPerformResource toResource(ExerciseToPerform exercise);

    @Named("todaySessionOrder")
    default Integer todaySessionOrder(Optional<WorkoutSession> todaySession) {
        return todaySession.map(WorkoutSession::getDayOrder).orElse(null);
    }

    @Named("todayWorkoutSessionId")
    default UUID todayWorkoutSessionId(Optional<WorkoutSession> todaySession) {
        return todaySession.map(session -> session.getId().value()).orElse(null);
    }

    // Monday first, as java.time.DayOfWeek declares them.
    default List<String> map(Set<DayOfWeek> trainingDays) {
        return trainingDays == null ? null : trainingDays.stream().sorted().map(DayOfWeek::name).toList();
    }

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(RoutineId routineId) {
        return routineId == null ? null : routineId.value();
    }

    default UUID map(ExerciseId exerciseId) {
        return exerciseId == null ? null : exerciseId.value();
    }
}
