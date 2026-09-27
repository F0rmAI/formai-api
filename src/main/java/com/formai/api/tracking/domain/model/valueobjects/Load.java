package com.formai.api.tracking.domain.model.valueobjects;

import com.formai.api.tracking.domain.exceptions.InvalidSetValueException;

import java.math.BigDecimal;

public record Load(BigDecimal kilograms) {

    public Load {
        if (kilograms == null || kilograms.signum() < 0) {
            throw new InvalidSetValueException("Load must be zero or more kilograms");
        }
    }
}
