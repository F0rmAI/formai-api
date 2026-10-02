package com.formai.api.clients.interfaces.rest.resources;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateBodyProfileResource(@NotBlank @Size(max = 120) String goal,
                                        @NotNull Integer heightCm,
                                        @NotNull @Digits(integer = 3, fraction = 2) BigDecimal weightKg,
                                        @Size(max = 500) String restrictions) { }
