package com.formai.api.clients.domain.model.valueobjects;

import com.formai.api.clients.domain.exceptions.InvalidBodyProfileException;

import java.math.BigDecimal;

public record BodyWeight(BigDecimal kilograms) {

    public BodyWeight {
        if (kilograms == null || kilograms.signum() <= 0) {
            throw new InvalidBodyProfileException("weightKg", "must be greater than 0 kg");
        }
    }
}
