package com.formai.api.iam.domain.model.valueobjects;

import com.formai.api.iam.domain.model.aggregates.User;

// Result of rotating a refresh token: the account to issue a new access JWT for, and the
// raw value of the refresh token that replaces the one just used.
public record RefreshedSession(User user, String refreshToken) { }
