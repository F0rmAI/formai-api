package com.formai.api.tracking.domain.exceptions;

public class WorkoutSessionAlreadyFinishedException extends RuntimeException {

    public WorkoutSessionAlreadyFinishedException() {
        super("This workout session is already closed and can no longer be changed");
    }
}
