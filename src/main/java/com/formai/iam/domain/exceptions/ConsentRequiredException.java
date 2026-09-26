package com.formai.iam.domain.exceptions;

public class ConsentRequiredException extends RuntimeException {

    public ConsentRequiredException() {
        super("You must accept the personal data processing terms to activate your account");
    }
}
