package com.formai.iam.domain.model.commands;

public record ActivateAccountCommand(String activationCode, String rawPassword, boolean consentAccepted) { }
