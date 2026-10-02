package com.formai.api.planning.domain.exceptions;

public class ExerciseAlreadyExistsException extends RuntimeException {

    public ExerciseAlreadyExistsException(String name) {
        super("An exercise named " + name + " already exists in your catalog");
    }
}
