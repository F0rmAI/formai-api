package com.formai.api.clients.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateClientResource(@NotBlank @Size(max = 120) String fullName) { }
