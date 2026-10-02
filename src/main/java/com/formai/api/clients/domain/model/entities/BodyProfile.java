package com.formai.api.clients.domain.model.entities;

import com.formai.api.clients.domain.model.valueobjects.BodyWeight;
import com.formai.api.clients.domain.model.valueobjects.BodyWeightRecord;
import com.formai.api.clients.domain.model.valueobjects.Height;
import com.formai.api.clients.domain.model.valueobjects.TrainingGoal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BodyProfile {

    private TrainingGoal goal;
    private Height height;
    private BodyWeight currentWeight;
    private String restrictions;
    private List<BodyWeightRecord> weightHistory = new ArrayList<>();

    public BodyProfile(TrainingGoal goal, Height height, String restrictions) {
        this.goal = goal;
        this.height = height;
        this.restrictions = restrictions;
    }

    public BodyProfile(TrainingGoal goal, Height height, BodyWeight currentWeight, String restrictions,
                       List<BodyWeightRecord> weightHistory) {
        this.goal = goal;
        this.height = height;
        this.currentWeight = currentWeight;
        this.restrictions = restrictions;
        this.weightHistory = new ArrayList<>(weightHistory);
    }

    public void recordWeight(BodyWeight weight, LocalDate on) {
        if (currentWeight != null && currentWeight.kilograms().compareTo(weight.kilograms()) == 0) {
            return;
        }
        this.currentWeight = weight;
        this.weightHistory.add(new BodyWeightRecord(weight, on));
    }

    public void update(TrainingGoal goal, Height height, String restrictions) {
        this.goal = goal;
        this.height = height;
        this.restrictions = restrictions;
    }

    public TrainingGoal getGoal() {
        return goal;
    }

    public Height getHeight() {
        return height;
    }

    public BodyWeight getCurrentWeight() {
        return currentWeight;
    }

    public String getRestrictions() {
        return restrictions;
    }

    public List<BodyWeightRecord> getWeightHistory() {
        return List.copyOf(weightHistory);
    }
}
