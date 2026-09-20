package com.formai.iam.domain.exceptions;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("An account with this email already exists: " + email);
    }
}
