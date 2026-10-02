package com.formai.api.clients.domain.model.valueobjects;

import com.formai.api.clients.domain.exceptions.InvalidBodyProfileException;

public record TrainingGoal(String value) {

    public TrainingGoal {
        if (value == null || value.isBlank() || value.strip().length() > 120) {
            throw new InvalidBodyProfileException("goal", "must have between 1 and 120 characters");
        }
        value = value.strip();
    }
}
