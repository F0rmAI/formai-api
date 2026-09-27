package com.formai.api.planning.domain.model.commands;

import com.formai.api.planning.domain.model.valueobjects.ExerciseName;
import com.formai.api.planning.domain.model.valueobjects.MuscleGroup;

import java.util.Optional;

public record CreateExerciseCommand(String holderId,
                                    ExerciseName name,
                                    MuscleGroup muscleGroup,
                                    Optional<String> equipment) { }
