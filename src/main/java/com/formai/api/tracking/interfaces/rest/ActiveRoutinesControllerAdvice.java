package com.formai.api.tracking.interfaces.rest;

import com.formai.api.tracking.domain.exceptions.ActiveRoutineNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.DateTimeException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ActiveRoutinesController.class)
public class ActiveRoutinesControllerAdvice {

    @ExceptionHandler(ActiveRoutineNotFoundException.class)
    public ProblemDetail handleActiveRoutineNotFound(ActiveRoutineNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ZoneId.of rejects an unknown or malformed X-Client-Timezone.
    @ExceptionHandler(DateTimeException.class)
    public ProblemDetail handleInvalidTimeZone(DateTimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "X-Client-Timezone must be an IANA time zone, e.g. America/Lima");
    }
}
