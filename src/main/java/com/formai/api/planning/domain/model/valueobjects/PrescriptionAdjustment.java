package com.formai.api.planning.domain.model.valueobjects;

import com.formai.api.planning.domain.exceptions.InvalidAdjustmentException;

import java.math.BigDecimal;
import java.util.List;

// One client's own target load and/or rest for an exercise of a routine session (FR-032): the
// routine stays the same for every client and the assignment carries what differs. A null value
// keeps the routine's.
public record PrescriptionAdjustment(int sessionOrder, ExerciseId exerciseId, BigDecimal targetLoadKg,
                                     Integer restSeconds) {

    public PrescriptionAdjustment {
        if (exerciseId == null) {
            throw new InvalidAdjustmentException("An adjustment needs the exercise it changes");
        }
        if (targetLoadKg == null && restSeconds == null) {
            throw new InvalidAdjustmentException("An adjustment changes the target load, the rest or both");
        }
        if ((targetLoadKg != null && targetLoadKg.signum() < 0) || (restSeconds != null && restSeconds < 0)) {
            throw new InvalidAdjustmentException("Target load and rest must be zero or more");
        }
    }

    public boolean appliesTo(int order, ExerciseId exercise) {
        return sessionOrder == order && exerciseId.equals(exercise);
    }

    public Prescription applyTo(Prescription prescription) {
        return new Prescription(prescription.sets(), prescription.reps(),
                targetLoadKg != null ? targetLoadKg : prescription.targetLoadKg(),
                restSeconds != null ? restSeconds : prescription.restSeconds());
    }

    // The prescription the client follows: the routine's, with the adjustment for that exercise of that
    // session if there is one. An adjustment whose exercise left the routine in a later version is ignored.
    public static Prescription personalize(List<PrescriptionAdjustment> adjustments, int sessionOrder,
                                           ExerciseId exerciseId, Prescription prescription) {
        return adjustments.stream()
                .filter(adjustment -> adjustment.appliesTo(sessionOrder, exerciseId))
                .findFirst()
                .map(adjustment -> adjustment.applyTo(prescription))
                .orElse(prescription);
    }
}
