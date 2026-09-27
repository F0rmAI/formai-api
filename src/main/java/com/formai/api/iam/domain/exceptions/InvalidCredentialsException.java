package com.formai.api.iam.domain.exceptions;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
