package com.formai.api.planning.interfaces.rest;

import com.formai.api.planning.domain.exceptions.AssigneeNotFoundException;
import com.formai.api.planning.domain.exceptions.ClientNotAssignableException;
import com.formai.api.planning.domain.exceptions.ExerciseNotFoundException;
import com.formai.api.planning.domain.exceptions.InvalidRoutineException;
import com.formai.api.planning.domain.exceptions.RoutineNotFoundException;
import com.formai.api.planning.domain.exceptions.TrainingDaysMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = RoutinesController.class)
public class RoutinesControllerAdvice {

    @ExceptionHandler(RoutineNotFoundException.class)
    public ProblemDetail handleRoutineNotFound(RoutineNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TrainingDaysMismatchException.class)
    public ProblemDetail handleTrainingDaysMismatch(TrainingDaysMismatchException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(InvalidRoutineException.class)
    public ProblemDetail handleInvalidRoutine(InvalidRoutineException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(ExerciseNotFoundException.class)
    public ProblemDetail handleExerciseNotFound(ExerciseNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AssigneeNotFoundException.class)
    public ProblemDetail handleAssigneeNotFound(AssigneeNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ClientNotAssignableException.class)
    public ProblemDetail handleClientNotAssignable(ClientNotAssignableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }
}
