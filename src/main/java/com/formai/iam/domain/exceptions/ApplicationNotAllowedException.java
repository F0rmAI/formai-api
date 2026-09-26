package com.formai.iam.domain.exceptions;

public class ApplicationNotAllowedException extends RuntimeException {

    public ApplicationNotAllowedException() {
        super("This account cannot sign in from this application. Client accounts must use the mobile app; trainer and administrator accounts must use the web platform");
    }
}
