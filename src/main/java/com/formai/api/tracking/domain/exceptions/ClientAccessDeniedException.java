package com.formai.api.tracking.domain.exceptions;

public class ClientAccessDeniedException extends RuntimeException {

    public ClientAccessDeniedException() {
        super("You do not have access to this client's workouts");
    }
}
