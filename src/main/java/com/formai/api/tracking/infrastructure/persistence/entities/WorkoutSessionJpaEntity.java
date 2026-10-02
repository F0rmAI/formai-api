package com.formai.api.tracking.infrastructure.persistence.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workout_sessions", schema = "tracking",
        uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "scheduled_for"}))
public class WorkoutSessionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "routine_id", nullable = false)
    private UUID routineId;

    @Column(name = "routine_version", nullable = false)
    private int routineVersion;

    @Column(name = "day_order", nullable = false)
    private int dayOrder;

    @Column(name = "day_label", nullable = false, length = 120)
    private String dayLabel;

    @Column(name = "scheduled_for", nullable = false)
    private LocalDate scheduledFor;

    // Stored as a plain string, not the domain ComplianceStatus type: this entity stays
    // framework-only and has zero dependency on the domain package.
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "finished_at")
    private Instant finishedAt;

    // Both collections are indexed (@OrderColumn) so they can be fetched eagerly together
    // without Hibernate's multiple-bag restriction, and the prescribed order is kept.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "workout_session_exercises", schema = "tracking",
            joinColumns = @JoinColumn(name = "workout_session_id"))
    @OrderColumn(name = "position")
    private List<PlannedExerciseEmbeddable> plannedExercises = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "workout_session_sets", schema = "tracking",
            joinColumns = @JoinColumn(name = "workout_session_id"))
    @OrderColumn(name = "position")
    private List<SetEntryEmbeddable> sets = new ArrayList<>();

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public WorkoutSessionJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public UUID getRoutineId() {
        return routineId;
    }

    public void setRoutineId(UUID routineId) {
        this.routineId = routineId;
    }

    public int getRoutineVersion() {
        return routineVersion;
    }

    public void setRoutineVersion(int routineVersion) {
        this.routineVersion = routineVersion;
    }

    public int getDayOrder() {
        return dayOrder;
    }

    public void setDayOrder(int dayOrder) {
        this.dayOrder = dayOrder;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public void setDayLabel(String dayLabel) {
        this.dayLabel = dayLabel;
    }

    public LocalDate getScheduledFor() {
        return scheduledFor;
    }

    public void setScheduledFor(LocalDate scheduledFor) {
        this.scheduledFor = scheduledFor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public List<PlannedExerciseEmbeddable> getPlannedExercises() {
        return plannedExercises;
    }

    public void setPlannedExercises(List<PlannedExerciseEmbeddable> plannedExercises) {
        this.plannedExercises = plannedExercises;
    }

    public List<SetEntryEmbeddable> getSets() {
        return sets;
    }

    public void setSets(List<SetEntryEmbeddable> sets) {
        this.sets = sets;
    }
}
