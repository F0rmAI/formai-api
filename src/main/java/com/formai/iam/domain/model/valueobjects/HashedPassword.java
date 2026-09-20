package com.formai.iam.domain.model.valueobjects;

public record HashedPassword(String value) {

    public HashedPassword {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be blank");
        }
    }
}
