package com.formai.api.tracking.domain.exceptions;

public class ActiveRoutineNotFoundException extends RuntimeException {

    public ActiveRoutineNotFoundException() {
        super("You have no routine assigned yet. Please contact your trainer");
    }
}
