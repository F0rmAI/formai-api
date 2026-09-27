package com.formai.api.clients.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.time.LocalDate;

@Embeddable
public class BodyWeightRecordEmbeddable {

    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "recorded_on", nullable = false)
    private LocalDate recordedOn;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public BodyWeightRecordEmbeddable() {
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public LocalDate getRecordedOn() {
        return recordedOn;
    }

    public void setRecordedOn(LocalDate recordedOn) {
        this.recordedOn = recordedOn;
    }
}
