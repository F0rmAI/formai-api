package studio.quedena.template.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignInResource(
        @NotBlank @Email String email,
        @NotBlank String password
) { }
