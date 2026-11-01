package com.formai.api.planning.domain.exceptions;

public class InvalidMuscleGroupException extends RuntimeException {

    public InvalidMuscleGroupException(String muscleGroup) {
        super("Muscle group " + muscleGroup + " is not one of the standard muscle groups");
    }
}
