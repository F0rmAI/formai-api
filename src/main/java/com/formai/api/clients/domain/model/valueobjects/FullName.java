package com.formai.api.clients.domain.model.valueobjects;

public record FullName(String value) {

    public FullName {
        if (value == null || value.isBlank() || value.strip().length() > 120) {
            throw new IllegalArgumentException("FullName must have between 1 and 120 characters");
        }
        value = value.strip();
    }
}
