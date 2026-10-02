package com.formai.api.iam.domain.model.valueobjects;

import java.util.regex.Pattern;

public record Email(String value) {

    public static final String FORMAT = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private static final Pattern PATTERN = Pattern.compile(FORMAT);

    public Email {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
        value = value.toLowerCase();
    }
}
