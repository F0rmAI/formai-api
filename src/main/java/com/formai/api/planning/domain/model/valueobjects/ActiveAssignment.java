package com.formai.api.planning.domain.model.valueobjects;

import com.formai.api.planning.domain.model.aggregates.Routine;
import com.formai.api.planning.domain.model.entities.PrescribedExercise;
import com.formai.api.planning.domain.model.entities.RoutineSession;

import java.time.LocalDate;
import java.util.List;

// The assignment a client follows: the routine at its current version, personalized with the
// assignment's adjustments, and the trainer's note (null when there is none).
public record ActiveAssignment(ClientId clientId, Routine routine, LocalDate startDate, TrainingDays trainingDays,
                               AssignmentNote note, List<PrescriptionAdjustment> adjustments) {

    public ActiveAssignment {
        adjustments = adjustments == null ? List.of() : List.copyOf(adjustments);
    }

    public ActiveAssignment(ClientId clientId, Routine routine, LocalDate startDate, TrainingDays trainingDays) {
        this(clientId, routine, startDate, trainingDays, null, List.of());
    }

    public List<RoutineSession> personalizedSessions() {
        return routine.currentVersion().getSessions().stream()
                .map(session -> new RoutineSession(session.getOrder(), session.getLabel(), session.getExercises()
                        .stream()
                        .map(exercise -> new PrescribedExercise(exercise.getExerciseId(), exercise.getExerciseName(),
                                PrescriptionAdjustment.personalize(adjustments, session.getOrder(),
                                        exercise.getExerciseId(), exercise.getPrescription())))
                        .toList()))
                .toList();
    }
}
