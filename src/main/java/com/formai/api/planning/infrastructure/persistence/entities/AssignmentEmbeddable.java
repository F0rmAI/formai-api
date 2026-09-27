package com.formai.api.planning.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;
import java.util.UUID;

@Embeddable
public class AssignmentEmbeddable {

    @Column(name = "routine_id", nullable = false)
    private UUID routineId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public AssignmentEmbeddable() {
    }

    public UUID getRoutineId() {
        return routineId;
    }

    public void setRoutineId(UUID routineId) {
        this.routineId = routineId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
