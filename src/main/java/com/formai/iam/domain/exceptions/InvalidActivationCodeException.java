package com.formai.iam.domain.exceptions;

public class InvalidActivationCodeException extends RuntimeException {

    public InvalidActivationCodeException() {
        super("The activation code is invalid, expired or already used. Please contact your trainer to get a new one");
    }
}
