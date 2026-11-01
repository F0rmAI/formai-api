package com.formai.api.planning.interfaces.rest;

import com.formai.api.planning.domain.exceptions.AssignmentNotFoundException;
import com.formai.api.planning.domain.exceptions.ClientAccessDeniedException;
import com.formai.api.planning.domain.exceptions.InvalidAdjustmentException;
import com.formai.api.planning.domain.exceptions.RoutineNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AssignmentsController.class)
public class AssignmentsControllerAdvice {

    @ExceptionHandler(ClientAccessDeniedException.class)
    public ProblemDetail handleClientAccessDenied(ClientAccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler({AssignmentNotFoundException.class, RoutineNotFoundException.class})
    public ProblemDetail handleAssignmentNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidAdjustmentException.class)
    public ProblemDetail handleInvalidAdjustment(InvalidAdjustmentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }
}
