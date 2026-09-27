package com.formai.api.clients.domain.exceptions;

// The email already belongs to another FormAI account; which one is never revealed.
public class ClientEmailUnavailableException extends RuntimeException {

    public ClientEmailUnavailableException() {
        super("This email is not available");
    }
}
