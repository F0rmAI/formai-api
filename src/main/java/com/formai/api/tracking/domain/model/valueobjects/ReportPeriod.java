package com.formai.api.tracking.domain.model.valueobjects;

import java.time.LocalDate;

public record ReportPeriod(LocalDate from, LocalDate to) {

    public ReportPeriod {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("A period needs a start date on or before its end date");
        }
    }
}
