package com.formai.api.tracking.interfaces.rest;

import com.formai.api.tracking.domain.exceptions.ActiveRoutineNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ActiveRoutinesController.class)
public class ActiveRoutinesControllerAdvice {

    @ExceptionHandler(ActiveRoutineNotFoundException.class)
    public ProblemDetail handleActiveRoutineNotFound(ActiveRoutineNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
