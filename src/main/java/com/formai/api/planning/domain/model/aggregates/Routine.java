package com.formai.api.planning.domain.model.aggregates;

import com.formai.api.planning.domain.exceptions.InvalidRoutineException;
import com.formai.api.planning.domain.exceptions.TrainingDaysMismatchException;
import com.formai.api.planning.domain.model.commands.CreateRoutineCommand;
import com.formai.api.planning.domain.model.commands.DuplicateRoutineCommand;
import com.formai.api.planning.domain.model.commands.UpdateRoutineCommand;
import com.formai.api.planning.domain.model.entities.RoutineSession;
import com.formai.api.planning.domain.model.entities.RoutineVersion;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.RoutineName;
import com.formai.api.planning.domain.model.valueobjects.RoutineStatus;
import com.formai.api.planning.domain.model.valueobjects.TrainingDays;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class Routine {

    private RoutineId id;
    private String holderId;
    private RoutineName name;
    private RoutineStatus status;
    private List<RoutineVersion> versions;
    private Instant createdAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public Routine() {
    }

    public static Routine create(CreateRoutineCommand command) {
        return draft(command.holderId(), command.name(), command.sessions());
    }

    public RoutineVersion revise(UpdateRoutineCommand command) {
        ensureValid(command.sessions());
        var version = new RoutineVersion(currentVersion().getNumber() + 1, Instant.now(), command.holderId(),
                command.sessions());
        this.name = command.name();
        this.versions = new ArrayList<>(versions);
        this.versions.add(version);
        return version;
    }

    public Routine duplicate(DuplicateRoutineCommand command) {
        return draft(command.holderId(), command.name(), currentVersion().getSessions());
    }

    public void markActive() {
        this.status = RoutineStatus.ACTIVE;
    }

    public void close() {
        if (status == RoutineStatus.ACTIVE) {
            this.status = RoutineStatus.CLOSED;
        }
    }

    public RoutineVersion currentVersion() {
        return versions.stream().max(Comparator.comparingInt(RoutineVersion::getNumber)).orElseThrow();
    }

    // One training day per session: each week the client goes through the whole routine once,
    // so a routine of two sessions is assigned on exactly two days of the week.
    public void ensureFits(TrainingDays trainingDays) {
        var sessions = currentVersion().getSessions().size();
        if (trainingDays.days().size() != sessions) {
            throw new TrainingDaysMismatchException(sessions, trainingDays.days().size());
        }
    }

    public boolean usesExercise(ExerciseId exerciseId) {
        return versions.stream()
                .flatMap(version -> version.getSessions().stream())
                .flatMap(session -> session.getExercises().stream())
                .anyMatch(exercise -> exercise.getExerciseId().equals(exerciseId));
    }

    private static Routine draft(String holderId, RoutineName name, List<RoutineSession> sessions) {
        ensureValid(sessions);
        var routine = new Routine();
        routine.id = new RoutineId(UUID.randomUUID());
        routine.holderId = holderId;
        routine.name = name;
        routine.status = RoutineStatus.DRAFT;
        routine.createdAt = Instant.now();
        routine.versions = new ArrayList<>(List.of(new RoutineVersion(1, routine.createdAt, holderId, sessions)));
        return routine;
    }

    private static void ensureValid(List<RoutineSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            throw new InvalidRoutineException("A routine needs at least one session");
        }
        sessions.stream()
                .filter(session -> session.getExercises().isEmpty())
                .findFirst()
                .ifPresent(session -> {
                    throw new InvalidRoutineException("Session '" + session.getLabel()
                            + "' needs at least one exercise");
                });
    }

    public RoutineId getId() {
        return id;
    }

    public String getHolderId() {
        return holderId;
    }

    public RoutineName getName() {
        return name;
    }

    public RoutineStatus getStatus() {
        return status;
    }

    public List<RoutineVersion> getVersions() {
        return versions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(RoutineId id) {
        this.id = id;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public void setName(RoutineName name) {
        this.name = name;
    }

    public void setStatus(RoutineStatus status) {
        this.status = status;
    }

    public void setVersions(List<RoutineVersion> versions) {
        this.versions = versions;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
