package studio.quedena.template.profiles.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileResource(
        @NotBlank @Size(max = 150) String firstName,
        @NotBlank @Size(max = 150) String lastName
) { }
