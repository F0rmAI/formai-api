package com.formai.api.planning.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "exercises", schema = "planning",
        uniqueConstraints = @UniqueConstraint(columnNames = {"holder_id", "name"}))
public class ExerciseJpaEntity {

    @Id
    private UUID id;

    @Column(name = "holder_id", nullable = false, length = 64)
    private String holderId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "muscle_group", nullable = false, length = 60)
    private String muscleGroup;

    @Column(name = "equipment", length = 120)
    private String equipment;

    // Stored as a plain string, not the domain ExerciseStatus type: this entity stays
    // framework-only and has zero dependency on the domain package.
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public ExerciseJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getHolderId() {
        return holderId;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMuscleGroup() {
        return muscleGroup;
    }

    public void setMuscleGroup(String muscleGroup) {
        this.muscleGroup = muscleGroup;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
