package com.formai.api.planning.domain.exceptions;

public class AssigneeNotFoundException extends RuntimeException {

    public AssigneeNotFoundException() {
        super("Client not found");
    }
}
