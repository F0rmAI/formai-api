package com.formai.api.tracking.domain.model.aggregates;

import com.formai.api.tracking.domain.exceptions.InvalidSetValueException;
import com.formai.api.tracking.domain.exceptions.PartialFinishNotConfirmedException;
import com.formai.api.tracking.domain.exceptions.WorkoutSessionAlreadyFinishedException;
import com.formai.api.tracking.domain.model.commands.CorrectSetCommand;
import com.formai.api.tracking.domain.model.commands.FinishWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.RecordSetCommand;
import com.formai.api.tracking.domain.model.commands.ScheduleWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.entities.SessionExercise;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.ComplianceStatus;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseId;
import com.formai.api.tracking.domain.model.valueobjects.RoutineDay;
import com.formai.api.tracking.domain.model.valueobjects.RoutineId;
import com.formai.api.tracking.domain.model.valueobjects.TrainingVolume;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class WorkoutSession {

    private WorkoutSessionId id;
    private ClientId clientId;
    private RoutineId routineId;
    private int routineVersion;
    private int dayOrder;
    private String dayLabel;
    private LocalDate scheduledFor;
    private ComplianceStatus status;
    private List<SessionExercise> exercises;
    private Instant finishedAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public WorkoutSession() {
    }

    public static WorkoutSession schedule(ScheduleWorkoutSessionCommand command, ActiveRoutine routine, RoutineDay day) {
        var session = new WorkoutSession();
        session.id = new WorkoutSessionId(UUID.randomUUID());
        session.clientId = command.clientId();
        session.routineId = routine.getRoutineId();
        session.routineVersion = routine.getVersion();
        session.dayOrder = day.order();
        session.dayLabel = day.label();
        session.scheduledFor = command.date();
        session.status = ComplianceStatus.PENDING;
        session.exercises = day.exercises().stream().map(SessionExercise::planned).toList();
        return session;
    }

    public void recordSet(RecordSetCommand command) {
        ensurePending();
        exercise(command.exerciseId()).record(command.setNumber(), command.load(), command.reps(), Instant.now());
    }

    public void correctSet(CorrectSetCommand command) {
        ensurePending();
        exercise(command.exerciseId()).correct(command.setNumber(), command.load(), command.reps());
    }

    public ComplianceStatus finish(FinishWorkoutSessionCommand command) {
        ensurePending();
        if (isComplete()) {
            status = ComplianceStatus.COMPLETED;
        } else if (command.confirmPartial()) {
            status = ComplianceStatus.PARTIAL;
        } else {
            throw new PartialFinishNotConfirmedException();
        }
        finishedAt = Instant.now();
        return status;
    }

    public void skip() {
        ensurePending();
        status = ComplianceStatus.SKIPPED;
    }

    // COMPLETED asks for every prescribed set of every exercise; anything less is PARTIAL.
    public boolean isComplete() {
        return exercises.stream().allMatch(SessionExercise::isComplete);
    }

    public boolean hasRecords() {
        return exercises.stream().anyMatch(SessionExercise::isRegistered);
    }

    public TrainingVolume volume() {
        return exercises.stream()
                .map(SessionExercise::volume)
                .reduce(TrainingVolume.ZERO, TrainingVolume::plus);
    }

    private void ensurePending() {
        if (status != ComplianceStatus.PENDING) {
            throw new WorkoutSessionAlreadyFinishedException();
        }
    }

    private SessionExercise exercise(ExerciseId exerciseId) {
        return exercises.stream()
                .filter(exercise -> exercise.getExerciseId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new InvalidSetValueException("This exercise is not part of the session"));
    }

    public WorkoutSessionId getId() {
        return id;
    }

    public ClientId getClientId() {
        return clientId;
    }

    public RoutineId getRoutineId() {
        return routineId;
    }

    public int getRoutineVersion() {
        return routineVersion;
    }

    public int getDayOrder() {
        return dayOrder;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public LocalDate getScheduledFor() {
        return scheduledFor;
    }

    public ComplianceStatus getStatus() {
        return status;
    }

    public List<SessionExercise> getExercises() {
        return exercises;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setId(WorkoutSessionId id) {
        this.id = id;
    }

    public void setClientId(ClientId clientId) {
        this.clientId = clientId;
    }

    public void setRoutineId(RoutineId routineId) {
        this.routineId = routineId;
    }

    public void setRoutineVersion(int routineVersion) {
        this.routineVersion = routineVersion;
    }

    public void setDayOrder(int dayOrder) {
        this.dayOrder = dayOrder;
    }

    public void setDayLabel(String dayLabel) {
        this.dayLabel = dayLabel;
    }

    public void setScheduledFor(LocalDate scheduledFor) {
        this.scheduledFor = scheduledFor;
    }

    public void setStatus(ComplianceStatus status) {
        this.status = status;
    }

    public void setExercises(List<SessionExercise> exercises) {
        this.exercises = exercises;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
