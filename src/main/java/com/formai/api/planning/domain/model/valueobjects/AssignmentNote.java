package com.formai.api.planning.domain.model.valueobjects;

import com.formai.api.planning.domain.exceptions.InvalidAdjustmentException;

// Optional free text the trainer leaves to the client with an assignment (FR-033).
public record AssignmentNote(String value) {

    public static final int MAX_LENGTH = 500;

    public AssignmentNote {
        if (value == null || value.isBlank()) {
            throw new InvalidAdjustmentException("A note cannot be blank");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidAdjustmentException("A note has at most " + MAX_LENGTH + " characters");
        }
    }
}
