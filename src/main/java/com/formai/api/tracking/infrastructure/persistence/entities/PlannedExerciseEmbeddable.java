package com.formai.api.tracking.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public class PlannedExerciseEmbeddable {

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "exercise_name", nullable = false, length = 120)
    private String exerciseName;

    @Column(name = "sets", nullable = false)
    private int sets;

    @Column(name = "reps", nullable = false)
    private int reps;

    @Column(name = "target_load_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal targetLoadKg;

    @Column(name = "rest_seconds", nullable = false)
    private int restSeconds;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public PlannedExerciseEmbeddable() {
    }

    public UUID getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(UUID exerciseId) {
        this.exerciseId = exerciseId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public int getSets() {
        return sets;
    }

    public void setSets(int sets) {
        this.sets = sets;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public BigDecimal getTargetLoadKg() {
        return targetLoadKg;
    }

    public void setTargetLoadKg(BigDecimal targetLoadKg) {
        this.targetLoadKg = targetLoadKg;
    }

    public int getRestSeconds() {
        return restSeconds;
    }

    public void setRestSeconds(int restSeconds) {
        this.restSeconds = restSeconds;
    }
}
