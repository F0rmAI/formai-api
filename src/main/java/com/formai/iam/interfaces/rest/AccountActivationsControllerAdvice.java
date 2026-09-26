package com.formai.iam.interfaces.rest;

import com.formai.iam.domain.exceptions.ConsentRequiredException;
import com.formai.iam.domain.exceptions.InvalidActivationCodeException;
import com.formai.iam.domain.exceptions.PasswordPolicyViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AccountActivationsController.class)
public class AccountActivationsControllerAdvice {

    @ExceptionHandler(InvalidActivationCodeException.class)
    public ProblemDetail handleInvalidActivationCode(InvalidActivationCodeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(ConsentRequiredException.class)
    public ProblemDetail handleConsentRequired(ConsentRequiredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(PasswordPolicyViolationException.class)
    public ProblemDetail handlePasswordPolicyViolation(PasswordPolicyViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }
}
