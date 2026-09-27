package com.formai.api.tracking.domain.model.entities;

import com.formai.api.tracking.domain.model.valueobjects.Load;
import com.formai.api.tracking.domain.model.valueobjects.Reps;
import com.formai.api.tracking.domain.model.valueobjects.TrainingVolume;

import java.time.Instant;

public class SetEntry {

    private final int setNumber;
    private final Load load;
    private final Reps reps;
    private final Instant recordedAt;

    public SetEntry(int setNumber, Load load, Reps reps, Instant recordedAt) {
        this.setNumber = setNumber;
        this.load = load;
        this.reps = reps;
        this.recordedAt = recordedAt;
    }

    public TrainingVolume volume() {
        return TrainingVolume.of(load, reps);
    }

    public int getSetNumber() {
        return setNumber;
    }

    public Load getLoad() {
        return load;
    }

    public Reps getReps() {
        return reps;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
