package com.formai.api.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record CreateActivationCodeVerificationResource(@NotBlank String activationCode) { }
