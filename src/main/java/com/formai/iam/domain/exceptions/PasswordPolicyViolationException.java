package com.formai.iam.domain.exceptions;

public class PasswordPolicyViolationException extends RuntimeException {

    public PasswordPolicyViolationException() {
        super("Password must be between 8 and 128 characters long");
    }
}
