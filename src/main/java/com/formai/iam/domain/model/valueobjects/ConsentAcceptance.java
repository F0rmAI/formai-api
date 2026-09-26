package com.formai.iam.domain.model.valueobjects;

import java.time.Instant;

public record ConsentAcceptance(Instant acceptedAt) {

    public ConsentAcceptance {
        if (acceptedAt == null) {
            throw new IllegalArgumentException("Consent acceptance date cannot be null");
        }
    }
}
