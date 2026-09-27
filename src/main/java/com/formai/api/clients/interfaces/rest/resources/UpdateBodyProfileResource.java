package com.formai.api.clients.interfaces.rest.resources;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// The ranges (weight above 0 kg, height between 100 and 250 cm) are business rules checked
// by the domain, which answers 422 naming the invalid field. restrictions is optional.
public record UpdateBodyProfileResource(@NotBlank @Size(max = 120) String goal,
                                        @NotNull Integer heightCm,
                                        @NotNull @Digits(integer = 3, fraction = 2) BigDecimal weightKg,
                                        @Size(max = 500) String restrictions) { }
