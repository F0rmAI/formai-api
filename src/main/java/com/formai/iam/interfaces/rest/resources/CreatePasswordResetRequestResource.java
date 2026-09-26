package com.formai.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreatePasswordResetRequestResource(@NotBlank @Email String email) { }
