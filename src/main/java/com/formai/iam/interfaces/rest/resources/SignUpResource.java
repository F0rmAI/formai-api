package com.formai.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpResource(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password
) { }
