package com.formai.api.tracking.domain.model.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Adherence(BigDecimal percentage) {

    public static final Adherence NONE = new Adherence(BigDecimal.ZERO);

    public static Adherence of(int completed, int scheduled) {
        if (scheduled == 0) {
            return NONE;
        }
        return new Adherence(BigDecimal.valueOf(completed * 100L)
                .divide(BigDecimal.valueOf(scheduled), 2, RoundingMode.HALF_UP));
    }
}
