package com.formai.api.tracking.domain.model.aggregates;

import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.valueobjects.ActiveRoutineId;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.PlannedRoutine;
import com.formai.api.tracking.domain.model.valueobjects.RoutineDay;
import com.formai.api.tracking.domain.model.valueobjects.RoutineId;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ActiveRoutine {

    // Used until the client's device reports its own zone: FormAI started with gyms in Peru.
    public static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("America/Lima");

    private ActiveRoutineId id;
    private ClientId clientId;
    private RoutineId routineId;
    private String routineName;
    private int version;
    private LocalDate startDate;
    private LocalDate endDate;
    private Set<DayOfWeek> trainingDays;
    private List<RoutineDay> days;
    private ZoneId timeZone;
    private LocalDate lastDailyRunOn;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public ActiveRoutine() {
    }

    public static ActiveRoutine syncFrom(SyncActiveRoutineCommand command, PlannedRoutine plan) {
        var routine = new ActiveRoutine();
        routine.id = new ActiveRoutineId(UUID.randomUUID());
        routine.clientId = command.clientId();
        routine.timeZone = DEFAULT_TIME_ZONE;
        routine.resync(plan);
        return routine;
    }

    public void resync(PlannedRoutine plan) {
        this.routineId = plan.routineId();
        this.routineName = plan.routineName();
        this.version = plan.version();
        this.startDate = plan.startDate();
        this.endDate = null;
        this.trainingDays = plan.trainingDays();
        this.days = plan.days();
    }

    public void end(EndActiveRoutineCommand command) {
        this.endDate = command.endDate();
    }

    // A calendar day starts at midnight in the client's own time zone, wherever they are.
    public LocalDate today(Instant now) {
        return LocalDate.ofInstant(now, timeZone);
    }

    // Returns whether the zone changed, so the caller saves only then.
    public boolean changeTimeZone(ZoneId zone) {
        if (zone.equals(timeZone)) {
            return false;
        }
        this.timeZone = zone;
        return true;
    }

    // The daily job runs every few minutes; each client's day is processed once, after its midnight.
    public boolean isDueForDailyRun(LocalDate today) {
        return lastDailyRunOn == null || lastDailyRunOn.isBefore(today);
    }

    public void recordDailyRun(LocalDate today) {
        this.lastDailyRunOn = today;
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

    // A session is scheduled only on the training days of the assignment: the rest days in
    // between are not sessions, so they never count as skipped.
    public boolean trainsOn(LocalDate date) {
        return isActiveOn(date) && trainingDays.contains(date.getDayOfWeek());
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

    public ZoneId getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(ZoneId timeZone) {
        this.timeZone = timeZone;
    }

    public LocalDate getLastDailyRunOn() {
        return lastDailyRunOn;
    }

    public void setLastDailyRunOn(LocalDate lastDailyRunOn) {
        this.lastDailyRunOn = lastDailyRunOn;
    }

    public Set<DayOfWeek> getTrainingDays() {
        return trainingDays;
    }

    public void setTrainingDays(Set<DayOfWeek> trainingDays) {
        this.trainingDays = trainingDays;
    }
}
