package com.formai.api.planning.domain.exceptions;

public class ClientNotAssignableException extends RuntimeException {

    public ClientNotAssignableException() {
        super("The client is not active, so no routine can be assigned to them");
    }
}
