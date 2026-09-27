package com.formai.api.planning.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoutineDuplicateResource(@NotBlank @Size(max = 120) String name) { }
