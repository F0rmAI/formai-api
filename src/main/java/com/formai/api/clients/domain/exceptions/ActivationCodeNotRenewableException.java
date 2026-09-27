package com.formai.api.clients.domain.exceptions;

public class ActivationCodeNotRenewableException extends RuntimeException {

    public ActivationCodeNotRenewableException() {
        super("Only a client who has not activated their account yet can get a new activation code");
    }
}
