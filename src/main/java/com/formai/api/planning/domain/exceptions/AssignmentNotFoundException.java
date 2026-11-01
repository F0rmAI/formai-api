package com.formai.api.planning.domain.exceptions;

public class AssignmentNotFoundException extends RuntimeException {

    public AssignmentNotFoundException() {
        super("The client has no current assignment");
    }
}
