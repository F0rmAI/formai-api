package com.formai.api.tracking.interfaces.rest.transform;

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

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ActiveRoutineAssembler {

    @Mapping(target = "routineId", source = "routine.routineId")
    @Mapping(target = "routineName", source = "routine.routineName")
    @Mapping(target = "version", source = "routine.version")
    @Mapping(target = "startDate", source = "routine.startDate")
    @Mapping(target = "sessions", source = "routine.days")
    @Mapping(target = "todaySessionOrder",
            expression = "java(plan.todaySession().map(session -> session.getDayOrder()).orElse(null))")
    @Mapping(target = "todayWorkoutSessionId",
            expression = "java(plan.todaySession().map(session -> session.getId().value()).orElse(null))")
    ActiveRoutineResource toResource(TodayPlan plan);

    RoutineDayResource toResource(RoutineDay day);

    ExerciseToPerformResource toResource(ExerciseToPerform exercise);

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(RoutineId routineId) {
        return routineId == null ? null : routineId.value();
    }

    default UUID map(ExerciseId exerciseId) {
        return exerciseId == null ? null : exerciseId.value();
    }
}
