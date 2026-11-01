package com.formai.api.planning.interfaces.rest.resources;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// trainingDays: java.time.DayOfWeek names (MONDAY … SUNDAY); missing or empty means every day.
// note: optional message of the trainer, the same for every client listed.
public record CreateAssignmentResource(@NotEmpty List<@NotNull UUID> clientIds, @NotNull LocalDate startDate,
                                       List<String> trainingDays, @Size(max = 500) String note) { }
