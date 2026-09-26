package com.formai.iam.domain.model.valueobjects;

/**
 * Closed set of platform-wide roles a User can accumulate.
 * <p>
 * REGISTERED_USER is the baseline every account gets. TRAINER and CLIENT tell the two
 * kinds of FormAI accounts apart. ADMINISTRATOR is additive, not a replacement — an
 * account can hold it together with its other roles. Granting a role never removes
 * another.
 */
public enum Role {
    REGISTERED_USER,
    ADMINISTRATOR,
    TRAINER,
    CLIENT
}
