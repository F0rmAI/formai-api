package com.formai.api.tracking.domain.model.valueobjects;

import com.formai.api.tracking.domain.exceptions.InvalidSetValueException;

public record Reps(int value) {

    public Reps {
        if (value < 0) {
            throw new InvalidSetValueException("Repetitions must be zero or more");
        }
    }
}
