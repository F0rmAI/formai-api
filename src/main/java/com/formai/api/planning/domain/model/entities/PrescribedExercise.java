package com.formai.api.planning.domain.model.entities;

import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.Prescription;

// A catalog exercise inside a routine session. The exercise name is copied when it is
// prescribed, so archiving or renaming the catalog entry never changes a routine (FR-009).
public class PrescribedExercise {

    private final ExerciseId exerciseId;
    private final String exerciseName;
    private final Prescription prescription;

    public PrescribedExercise(ExerciseId exerciseId, String exerciseName, Prescription prescription) {
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.prescription = prescription;
    }

    public PrescribedExercise named(String exerciseName) {
        return new PrescribedExercise(exerciseId, exerciseName, prescription);
    }

    public ExerciseId getExerciseId() {
        return exerciseId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public Prescription getPrescription() {
        return prescription;
    }
}
