package com.formai.api.tracking.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Embeddable
public class SetEntryEmbeddable {

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(name = "load_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal loadKg;

    @Column(name = "reps", nullable = false)
    private int reps;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public SetEntryEmbeddable() {
    }

    public UUID getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(UUID exerciseId) {
        this.exerciseId = exerciseId;
    }

    public int getSetNumber() {
        return setNumber;
    }

    public void setSetNumber(int setNumber) {
        this.setNumber = setNumber;
    }

    public BigDecimal getLoadKg() {
        return loadKg;
    }

    public void setLoadKg(BigDecimal loadKg) {
        this.loadKg = loadKg;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
