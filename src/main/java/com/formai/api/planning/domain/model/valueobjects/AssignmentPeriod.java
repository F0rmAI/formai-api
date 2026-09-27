package com.formai.api.planning.domain.model.valueobjects;

import java.time.LocalDate;

// endDate is null while the assignment is still in effect.
public record AssignmentPeriod(LocalDate startDate, LocalDate endDate) {

    public AssignmentPeriod {
        if (startDate == null) {
            throw new IllegalArgumentException("An assignment needs a start date");
        }
    }

    public static AssignmentPeriod startingOn(LocalDate startDate) {
        return new AssignmentPeriod(startDate, null);
    }

    public AssignmentPeriod endingOn(LocalDate endDate) {
        return new AssignmentPeriod(startDate, endDate);
    }
}
