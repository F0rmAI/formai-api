package com.formai.api.clients.domain.exceptions;

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
