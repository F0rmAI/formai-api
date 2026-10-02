package com.formai.api.clients.interfaces.rest;

import com.formai.api.clients.domain.exceptions.ActivationCodeNotRenewableException;
import com.formai.api.clients.domain.exceptions.ClientNotFoundException;
import com.formai.api.clients.domain.exceptions.InvalidBodyProfileException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ClientsController.class)
public class ClientsControllerAdvice {

    @ExceptionHandler(ClientNotFoundException.class)
    public ProblemDetail handleClientNotFound(ClientNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ActivationCodeNotRenewableException.class)
    public ProblemDetail handleActivationCodeNotRenewable(ActivationCodeNotRenewableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidBodyProfileException.class)
    public ProblemDetail handleInvalidBodyProfile(InvalidBodyProfileException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setProperty("field", ex.getField());
        return problem;
    }
}
