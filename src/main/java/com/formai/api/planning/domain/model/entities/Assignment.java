package com.formai.api.planning.domain.model.entities;

import com.formai.api.planning.domain.exceptions.InvalidAdjustmentException;
import com.formai.api.planning.domain.model.valueobjects.AssignmentNote;
import com.formai.api.planning.domain.model.valueobjects.AssignmentPeriod;
import com.formai.api.planning.domain.model.valueobjects.PrescriptionAdjustment;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.TrainingDays;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

public class Assignment {

    private final RoutineId routineId;
    private final TrainingDays trainingDays;
    private AssignmentPeriod period;
    // Optional: null when the trainer left no note.
    private AssignmentNote note;
    private List<PrescriptionAdjustment> adjustments;

    public Assignment(RoutineId routineId, AssignmentPeriod period, TrainingDays trainingDays, AssignmentNote note,
                      List<PrescriptionAdjustment> adjustments) {
        this.routineId = routineId;
        this.period = period;
        this.trainingDays = trainingDays;
        this.note = note;
        this.adjustments = adjustments == null ? List.of() : List.copyOf(adjustments);
    }

    public void close(LocalDate endDate) {
        this.period = period.endingOn(endDate);
    }

    // Each adjustment must point at an exercise of a session of the routine version the client
    // follows, once at most.
    public void personalize(AssignmentNote note, List<PrescriptionAdjustment> adjustments, RoutineVersion version) {
        var seen = new HashSet<String>();
        for (var adjustment : adjustments) {
            if (!seen.add(adjustment.sessionOrder() + "/" + adjustment.exerciseId().value())) {
                throw new InvalidAdjustmentException("An exercise of a session is adjusted only once");
            }
            var prescribed = version.getSessions().stream()
                    .filter(session -> session.getOrder() == adjustment.sessionOrder())
                    .flatMap(session -> session.getExercises().stream())
                    .anyMatch(exercise -> exercise.getExerciseId().equals(adjustment.exerciseId()));
            if (!prescribed) {
                throw new InvalidAdjustmentException("Session " + adjustment.sessionOrder()
                        + " of the routine does not prescribe exercise " + adjustment.exerciseId().value());
            }
        }
        this.note = note;
        this.adjustments = List.copyOf(adjustments);
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

    public AssignmentNote getNote() {
        return note;
    }

    public List<PrescriptionAdjustment> getAdjustments() {
        return adjustments;
    }
}
