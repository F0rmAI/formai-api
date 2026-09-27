package com.formai.api.clients.domain.exceptions;

// Carries the name of the invalid field so the response can point at it (FR-007).
public class InvalidBodyProfileException extends RuntimeException {

    private final String field;

    public InvalidBodyProfileException(String field, String reason) {
        super(field + " " + reason);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
