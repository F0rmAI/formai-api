package com.formai.api.tracking.domain.model.aggregates;

import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.valueobjects.ActiveRoutineId;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.PlannedRoutine;
import com.formai.api.tracking.domain.model.valueobjects.RoutineDay;
import com.formai.api.tracking.domain.model.valueobjects.RoutineId;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ActiveRoutine {

    private ActiveRoutineId id;
    private ClientId clientId;
    private RoutineId routineId;
    private String routineName;
    private int version;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<RoutineDay> days;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public ActiveRoutine() {
    }

    public static ActiveRoutine syncFrom(SyncActiveRoutineCommand command, PlannedRoutine plan) {
        var routine = new ActiveRoutine();
        routine.id = new ActiveRoutineId(UUID.randomUUID());
        routine.clientId = command.clientId();
        routine.resync(plan);
        return routine;
    }

    public void resync(PlannedRoutine plan) {
        this.routineId = plan.routineId();
        this.routineName = plan.routineName();
        this.version = plan.version();
        this.startDate = plan.startDate();
        this.endDate = null;
        this.days = plan.days();
    }

    public void end(EndActiveRoutineCommand command) {
        this.endDate = command.endDate();
    }

    public RoutineDay nextDay(Optional<Integer> lastOrder) {
        var ordered = days.stream().sorted(Comparator.comparingInt(RoutineDay::order)).toList();
        return lastOrder
                .flatMap(last -> ordered.stream().filter(day -> day.order() > last).findFirst())
                .orElse(ordered.getFirst());
    }

    public boolean isActiveOn(LocalDate date) {
        return !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate));
    }

    public ActiveRoutineId getId() {
        return id;
    }

    public ClientId getClientId() {
        return clientId;
    }

    public RoutineId getRoutineId() {
        return routineId;
    }

    public String getRoutineName() {
        return routineName;
    }

    public int getVersion() {
        return version;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<RoutineDay> getDays() {
        return days;
    }

    public void setId(ActiveRoutineId id) {
        this.id = id;
    }

    public void setClientId(ClientId clientId) {
        this.clientId = clientId;
    }

    public void setRoutineId(RoutineId routineId) {
        this.routineId = routineId;
    }

    public void setRoutineName(String routineName) {
        this.routineName = routineName;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setDays(List<RoutineDay> days) {
        this.days = days;
    }
}
