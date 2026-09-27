package com.formai.api.planning.domain.model.commands;

import com.formai.api.planning.domain.model.valueobjects.ExerciseId;

public record RestoreExerciseCommand(ExerciseId exerciseId, String holderId) { }
