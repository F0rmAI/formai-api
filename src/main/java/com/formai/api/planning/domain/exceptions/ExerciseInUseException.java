package com.formai.api.planning.domain.exceptions;

public class ExerciseInUseException extends RuntimeException {

    public ExerciseInUseException() {
        super("This exercise is used in a routine: archive it instead of deleting it");
    }
}
