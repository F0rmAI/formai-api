package com.formai.api.planning.domain.model.aggregates;

import com.formai.api.planning.domain.model.commands.CreateExerciseCommand;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.ExerciseName;
import com.formai.api.planning.domain.model.valueobjects.ExerciseStatus;
import com.formai.api.planning.domain.model.valueobjects.MuscleGroup;

import java.util.UUID;

public class Exercise {

    private ExerciseId id;
    private String holderId;
    private ExerciseName name;
    private MuscleGroup muscleGroup;
    private String equipment;
    private ExerciseStatus status;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public Exercise() {
    }

    public static Exercise create(CreateExerciseCommand command) {
        var exercise = new Exercise();
        exercise.id = new ExerciseId(UUID.randomUUID());
        exercise.holderId = command.holderId();
        exercise.name = command.name();
        exercise.muscleGroup = command.muscleGroup();
        exercise.equipment = command.equipment().filter(value -> !value.isBlank()).map(String::strip).orElse(null);
        exercise.status = ExerciseStatus.ACTIVE;
        return exercise;
    }

    public void archive() {
        this.status = ExerciseStatus.ARCHIVED;
    }

    public void restore() {
        this.status = ExerciseStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == ExerciseStatus.ACTIVE;
    }

    public ExerciseId getId() {
        return id;
    }

    public String getHolderId() {
        return holderId;
    }

    public ExerciseName getName() {
        return name;
    }

    public MuscleGroup getMuscleGroup() {
        return muscleGroup;
    }

    public String getEquipment() {
        return equipment;
    }

    public ExerciseStatus getStatus() {
        return status;
    }

    public void setId(ExerciseId id) {
        this.id = id;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public void setName(ExerciseName name) {
        this.name = name;
    }

    public void setMuscleGroup(MuscleGroup muscleGroup) {
        this.muscleGroup = muscleGroup;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public void setStatus(ExerciseStatus status) {
        this.status = status;
    }
}
