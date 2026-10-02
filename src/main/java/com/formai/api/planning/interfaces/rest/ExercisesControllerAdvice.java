package com.formai.api.planning.interfaces.rest;

import com.formai.api.planning.domain.exceptions.ExerciseAlreadyExistsException;
import com.formai.api.planning.domain.exceptions.ExerciseInUseException;
import com.formai.api.planning.domain.exceptions.ExerciseNotFoundException;
import com.formai.api.planning.domain.exceptions.MachineNotPublishedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ExercisesController.class)
public class ExercisesControllerAdvice {

    @ExceptionHandler(ExerciseAlreadyExistsException.class)
    public ProblemDetail handleExerciseAlreadyExists(ExerciseAlreadyExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ExerciseNotFoundException.class)
    public ProblemDetail handleExerciseNotFound(ExerciseNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ExerciseInUseException.class)
    public ProblemDetail handleExerciseInUse(ExerciseInUseException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MachineNotPublishedException.class)
    public ProblemDetail handleMachineNotPublished(MachineNotPublishedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }
}
