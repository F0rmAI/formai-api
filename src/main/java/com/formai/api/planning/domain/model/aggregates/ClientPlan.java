package com.formai.api.planning.domain.model.aggregates;

import com.formai.api.planning.domain.exceptions.AssignmentNotFoundException;
import com.formai.api.planning.domain.model.commands.AssignRoutineCommand;
import com.formai.api.planning.domain.model.commands.PersonalizeAssignmentCommand;
import com.formai.api.planning.domain.model.commands.TransferClientPlanCommand;
import com.formai.api.planning.domain.model.entities.Assignment;
import com.formai.api.planning.domain.model.entities.RoutineVersion;
import com.formai.api.planning.domain.model.valueobjects.AssignmentPeriod;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.model.valueobjects.ClientPlanId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ClientPlan {

    private ClientPlanId id;
    private ClientId clientId;
    private String holderId;
    private List<Assignment> assignments;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public ClientPlan() {
    }

    public static ClientPlan startFor(ClientId clientId, String holderId) {
        var plan = new ClientPlan();
        plan.id = new ClientPlanId(UUID.randomUUID());
        plan.clientId = clientId;
        plan.holderId = holderId;
        plan.assignments = new ArrayList<>();
        return plan;
    }

    public Assignment assign(AssignRoutineCommand command) {
        closeCurrentAssignmentBefore(command.startDate());
        // Whoever assigns is the client's trainer (checked before): the plan follows that trainer,
        // which also repairs a plan left with the previous one after a failed transfer.
        this.holderId = command.holderId();
        var assignment = new Assignment(command.routineId(), AssignmentPeriod.startingOn(command.startDate()),
                command.trainingDays(), command.note().orElse(null), List.of());
        this.assignments = new ArrayList<>(assignments);
        this.assignments.add(assignment);
        return assignment;
    }

    // The trainer adapts the current assignment to this client (FR-032, FR-033) without touching the
    // routine, which other clients may follow too.
    public Assignment personalizeCurrentAssignment(PersonalizeAssignmentCommand command, RoutineVersion version) {
        var current = currentAssignment().orElseThrow(AssignmentNotFoundException::new);
        current.personalize(command.note().orElse(null), command.adjustments(), version);
        return current;
    }

    public void closeCurrentAssignment(LocalDate endDate) {
        currentAssignment().ifPresent(current -> current.close(endDate));
    }

    // The client joined another trainer: the routine of the previous one stops applying and the
    // whole assignment history goes with the client.
    public void transferTo(TransferClientPlanCommand command) {
        closeCurrentAssignmentBefore(command.date());
        this.holderId = command.newHolderId();
    }

    // The assignment ends the day before, so it no longer applies on that date; one that started
    // that same day cannot end before it started and ends on its start date.
    private void closeCurrentAssignmentBefore(LocalDate date) {
        currentAssignment().ifPresent(current -> {
            var endDate = date.minusDays(1);
            var start = current.getPeriod().startDate();
            current.close(endDate.isBefore(start) ? start : endDate);
        });
    }

    public Optional<Assignment> currentAssignment() {
        return assignments.stream().filter(Assignment::isCurrent).findFirst();
    }

    public ClientPlanId getId() {
        return id;
    }

    public ClientId getClientId() {
        return clientId;
    }

    public String getHolderId() {
        return holderId;
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void setId(ClientPlanId id) {
        this.id = id;
    }

    public void setClientId(ClientId clientId) {
        this.clientId = clientId;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }
}
