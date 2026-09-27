package com.formai.api.iam.domain.model.valueobjects;

import java.time.Instant;

public record FailedSignInAttempts(int count, Instant lockedUntil) {

    public FailedSignInAttempts {
        if (count < 0) {
            throw new IllegalArgumentException("Failed sign-in count cannot be negative");
        }
    }
}
