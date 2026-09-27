package com.formai.api.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record CreateAccountActivationResource(
        @NotBlank String activationCode,
        @NotBlank String password,
        boolean consentAccepted
) { }
