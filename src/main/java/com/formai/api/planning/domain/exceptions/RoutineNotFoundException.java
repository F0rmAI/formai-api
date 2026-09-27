package com.formai.api.planning.domain.exceptions;

public class RoutineNotFoundException extends RuntimeException {

    public RoutineNotFoundException() {
        super("Routine not found");
    }
}
