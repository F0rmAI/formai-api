package com.formai.api.planning.domain.model.entities;

import com.formai.api.planning.domain.model.valueobjects.AssignmentPeriod;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.TrainingDays;

import java.time.LocalDate;

public class Assignment {

    private final RoutineId routineId;
    private final TrainingDays trainingDays;
    private AssignmentPeriod period;

    public Assignment(RoutineId routineId, AssignmentPeriod period, TrainingDays trainingDays) {
        this.routineId = routineId;
        this.period = period;
        this.trainingDays = trainingDays;
    }

    public void close(LocalDate endDate) {
        this.period = period.endingOn(endDate);
    }

    public boolean isCurrent() {
        return period.endDate() == null;
    }

    public RoutineId getRoutineId() {
        return routineId;
    }

    public AssignmentPeriod getPeriod() {
        return period;
    }

    public TrainingDays getTrainingDays() {
        return trainingDays;
    }
}
