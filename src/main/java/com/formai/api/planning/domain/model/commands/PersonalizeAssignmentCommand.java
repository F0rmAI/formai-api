package com.formai.api.planning.domain.model.commands;

import com.formai.api.planning.domain.model.valueobjects.AssignmentNote;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.model.valueobjects.PrescriptionAdjustment;

import java.util.List;
import java.util.Optional;

// Replaces the note and the adjustments of the client's current assignment.
public record PersonalizeAssignmentCommand(ClientId clientId, String holderId, Optional<AssignmentNote> note,
                                           List<PrescriptionAdjustment> adjustments) { }
