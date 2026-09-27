package com.formai.api.planning.domain.model.valueobjects;

import com.formai.api.planning.domain.exceptions.InvalidRoutineException;

import java.math.BigDecimal;

// What the trainer prescribes for one exercise: sets and reps must be greater than zero,
// target load and rest zero or more (FR-008).
public record Prescription(int sets, int reps, BigDecimal targetLoadKg, int restSeconds) {

    public Prescription {
        if (sets <= 0 || reps <= 0) {
            throw new InvalidRoutineException("Sets and repetitions must be greater than zero");
        }
        if (targetLoadKg == null || targetLoadKg.signum() < 0 || restSeconds < 0) {
            throw new InvalidRoutineException("Target load and rest must be zero or more");
        }
    }
}
