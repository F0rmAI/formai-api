package com.formai.api.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignUpResource(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank String password
) { }
