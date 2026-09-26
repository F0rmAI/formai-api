package com.formai.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record CreatePasswordResetResource(
        @NotBlank String token,
        @NotBlank String password
) { }
