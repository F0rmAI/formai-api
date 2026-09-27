package com.formai.api.planning.interfaces.rest.resources;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateRoutineResource(@NotBlank @Size(max = 120) String name,
                                    @NotNull @Valid List<CreateRoutineSessionResource> sessions) { }
