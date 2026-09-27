package com.formai.api.tracking.domain.model.entities;

import com.formai.api.tracking.domain.exceptions.InvalidSetValueException;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseId;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseToPerform;
import com.formai.api.tracking.domain.model.valueobjects.Load;
import com.formai.api.tracking.domain.model.valueobjects.Reps;
import com.formai.api.tracking.domain.model.valueobjects.TrainingVolume;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SessionExercise {

    private final ExerciseId exerciseId;
    private final ExerciseToPerform toPerform;
    private final List<SetEntry> sets;

    public SessionExercise(ExerciseToPerform toPerform, List<SetEntry> sets) {
        this.exerciseId = toPerform.exerciseId();
        this.toPerform = toPerform;
        this.sets = new ArrayList<>(sets);
        this.sets.sort(Comparator.comparingInt(SetEntry::getSetNumber));
    }

    public static SessionExercise planned(ExerciseToPerform toPerform) {
        return new SessionExercise(toPerform, List.of());
    }

    public void record(int setNumber, Load load, Reps reps, Instant at) {
        ensurePrescribed(setNumber);
        sets.removeIf(set -> set.getSetNumber() == setNumber);
        sets.add(new SetEntry(setNumber, load, reps, at));
        sets.sort(Comparator.comparingInt(SetEntry::getSetNumber));
    }

    public void correct(int setNumber, Load load, Reps reps) {
        ensurePrescribed(setNumber);
        var index = indexOf(setNumber);
        if (index < 0) {
            throw new InvalidSetValueException("Set " + setNumber + " of " + toPerform.exerciseName()
                    + " has not been recorded yet");
        }
        sets.set(index, new SetEntry(setNumber, load, reps, sets.get(index).getRecordedAt()));
    }

    public boolean isRegistered() {
        return !sets.isEmpty();
    }

    public TrainingVolume volume() {
        return sets.stream()
                .map(SetEntry::volume)
                .reduce(TrainingVolume.ZERO, TrainingVolume::plus);
    }

    public Load maxLoad() {
        return sets.stream()
                .map(SetEntry::getLoad)
                .max(Comparator.comparing(Load::kilograms))
                .orElse(new Load(BigDecimal.ZERO));
    }

    private void ensurePrescribed(int setNumber) {
        if (setNumber < 1 || setNumber > toPerform.sets()) {
            throw new InvalidSetValueException("Set number must be between 1 and " + toPerform.sets()
                    + " for " + toPerform.exerciseName());
        }
    }

    private int indexOf(int setNumber) {
        for (int i = 0; i < sets.size(); i++) {
            if (sets.get(i).getSetNumber() == setNumber) {
                return i;
            }
        }
        return -1;
    }

    public ExerciseId getExerciseId() {
        return exerciseId;
    }

    public ExerciseToPerform getToPerform() {
        return toPerform;
    }

    public List<SetEntry> getSets() {
        return List.copyOf(sets);
    }
}
