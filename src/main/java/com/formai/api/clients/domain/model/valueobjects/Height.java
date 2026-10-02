package com.formai.api.clients.domain.model.valueobjects;

import com.formai.api.clients.domain.exceptions.InvalidBodyProfileException;

public record Height(int centimeters) {

    public static final int MIN_CENTIMETERS = 100;
    public static final int MAX_CENTIMETERS = 250;

    public Height {
        if (centimeters < MIN_CENTIMETERS || centimeters > MAX_CENTIMETERS) {
            throw new InvalidBodyProfileException("heightCm", "must be between 100 and 250 cm");
        }
    }
}
