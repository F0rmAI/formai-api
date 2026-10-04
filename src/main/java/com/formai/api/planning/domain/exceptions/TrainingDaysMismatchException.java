package com.formai.api.planning.domain.exceptions;

public class TrainingDaysMismatchException extends RuntimeException {

    public TrainingDaysMismatchException(int sessions, int trainingDays) {
        super("The routine has " + sessions + " session(s), so the assignment needs exactly " + sessions
                + " training day(s); " + trainingDays + " were chosen");
    }
}
