package com.formai.api.planning.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExerciseResource(@NotBlank @Size(max = 120) String name,
                                     @NotBlank @Size(max = 60) String muscleGroup,
                                     @Size(max = 120) String equipment) { }
