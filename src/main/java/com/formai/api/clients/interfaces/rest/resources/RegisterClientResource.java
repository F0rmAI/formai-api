package com.formai.api.clients.interfaces.rest.resources;

import com.formai.api.clients.domain.model.valueobjects.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterClientResource(@NotBlank @Size(max = 120) String fullName,
                                     @NotBlank @Size(max = 254)
                                     @jakarta.validation.constraints.Email(regexp = Email.FORMAT) String email) { }
