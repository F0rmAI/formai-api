package com.formai.api.tracking.interfaces.rest;

import com.formai.api.tracking.domain.exceptions.ClientAccessDeniedException;
import com.formai.api.tracking.domain.exceptions.WorkoutSessionNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ClientOverviewsController.class)
public class ClientOverviewsControllerAdvice {

    @ExceptionHandler(ClientAccessDeniedException.class)
    public ProblemDetail handleClientAccessDenied(ClientAccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(WorkoutSessionNotFoundException.class)
    public ProblemDetail handleWorkoutSessionNotFound(WorkoutSessionNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
