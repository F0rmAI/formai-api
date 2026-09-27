package com.formai.api.tracking.interfaces.rest;

import com.formai.api.tracking.domain.exceptions.ActiveRoutineNotFoundException;
import com.formai.api.tracking.domain.exceptions.ClientAccessDeniedException;
import com.formai.api.tracking.domain.exceptions.InvalidSetValueException;
import com.formai.api.tracking.domain.exceptions.PartialFinishNotConfirmedException;
import com.formai.api.tracking.domain.exceptions.WorkoutSessionAlreadyFinishedException;
import com.formai.api.tracking.domain.exceptions.WorkoutSessionNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = WorkoutSessionsController.class)
public class WorkoutSessionsControllerAdvice {

    @ExceptionHandler(WorkoutSessionNotFoundException.class)
    public ProblemDetail handleWorkoutSessionNotFound(WorkoutSessionNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(WorkoutSessionAlreadyFinishedException.class)
    public ProblemDetail handleWorkoutSessionAlreadyFinished(WorkoutSessionAlreadyFinishedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidSetValueException.class)
    public ProblemDetail handleInvalidSetValue(InvalidSetValueException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(PartialFinishNotConfirmedException.class)
    public ProblemDetail handlePartialFinishNotConfirmed(PartialFinishNotConfirmedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ClientAccessDeniedException.class)
    public ProblemDetail handleClientAccessDenied(ClientAccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ActiveRoutineNotFoundException.class)
    public ProblemDetail handleActiveRoutineNotFound(ActiveRoutineNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
