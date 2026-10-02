package com.formai.api.iam.domain.model.commands;

import com.formai.api.iam.domain.model.valueobjects.Email;

public record ActivateAccountCommand(String activationCode, Email email, String rawPassword,
                                     boolean consentAccepted, String consentVersion) { }
