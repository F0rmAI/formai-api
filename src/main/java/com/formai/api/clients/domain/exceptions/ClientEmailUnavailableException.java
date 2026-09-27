package com.formai.api.clients.domain.exceptions;

public class ClientEmailUnavailableException extends RuntimeException {

    public ClientEmailUnavailableException() {
        super("This email is not available");
    }
}
