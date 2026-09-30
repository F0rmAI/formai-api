package com.formai.api.iam.domain.model.valueobjects;

import java.time.Instant;

// NFR-021: the consent is recorded with the version of the text the client accepted and when.
public record ConsentAcceptance(String version, Instant acceptedAt) {

    public static final int MAX_VERSION_LENGTH = 20;

    public ConsentAcceptance {
        if (version == null || version.isBlank() || version.length() > MAX_VERSION_LENGTH) {
            throw new IllegalArgumentException("Consent version must have between 1 and "
                    + MAX_VERSION_LENGTH + " characters");
        }
        if (acceptedAt == null) {
            throw new IllegalArgumentException("Consent acceptance date cannot be null");
        }
    }
}
