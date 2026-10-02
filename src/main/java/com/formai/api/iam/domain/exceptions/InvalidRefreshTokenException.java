package com.formai.api.iam.domain.exceptions;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("The session has expired. Sign in again.");
    }
}
