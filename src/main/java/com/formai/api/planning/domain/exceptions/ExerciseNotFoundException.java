package com.formai.api.planning.domain.exceptions;

public class ExerciseNotFoundException extends RuntimeException {

    public ExerciseNotFoundException() {
        super("Exercise not found");
    }
}
