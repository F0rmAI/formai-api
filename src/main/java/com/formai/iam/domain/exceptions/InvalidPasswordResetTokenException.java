package com.formai.iam.domain.exceptions;

public class InvalidPasswordResetTokenException extends RuntimeException {

    public InvalidPasswordResetTokenException() {
        super("The password reset link is invalid, expired or already used. Please request a new one");
    }
}
