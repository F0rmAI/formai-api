package com.formai.api.tracking.domain.model.valueobjects;

import java.math.BigDecimal;

// Volume = load × repetitions, summed over the sets it covers.
public record TrainingVolume(BigDecimal kilograms) {

    public static final TrainingVolume ZERO = new TrainingVolume(BigDecimal.ZERO);

    public static TrainingVolume of(Load load, Reps reps) {
        return new TrainingVolume(load.kilograms().multiply(BigDecimal.valueOf(reps.value())));
    }

    public TrainingVolume plus(TrainingVolume other) {
        return new TrainingVolume(kilograms.add(other.kilograms()));
    }
}
