package com.formai.api.planning.interfaces.rest.resources;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// Replaces the note and the adjustments of the current assignment: a blank note removes it, a
// missing or empty list removes every adjustment.
public record UpdateAssignmentResource(@Size(max = 500) String note,
                                       List<@Valid @NotNull PrescriptionAdjustmentResource> adjustments) { }
