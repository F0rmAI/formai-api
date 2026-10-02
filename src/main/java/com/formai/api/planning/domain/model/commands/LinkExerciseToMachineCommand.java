package com.formai.api.planning.domain.model.commands;

import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.MachineId;

public record LinkExerciseToMachineCommand(ExerciseId exerciseId, String holderId, MachineId machineId) { }
