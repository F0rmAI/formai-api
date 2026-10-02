package com.formai.api.planning.domain.model.aggregates;

import com.formai.api.planning.domain.model.commands.AssignRoutineCommand;
import com.formai.api.planning.domain.model.commands.TransferClientPlanCommand;
import com.formai.api.planning.domain.model.entities.Assignment;
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
        currentAssignment().ifPresent(current -> {
            var endDate = command.startDate().minusDays(1);
            var previousStart = current.getPeriod().startDate();
            current.close(endDate.isBefore(previousStart) ? previousStart : endDate);
        });
        // Whoever assigns is the client's trainer (checked before): the plan follows that trainer,
        // which also repairs a plan left with the previous one after a failed transfer.
        this.holderId = command.holderId();
        var assignment = new Assignment(command.routineId(), AssignmentPeriod.startingOn(command.startDate()));
        this.assignments = new ArrayList<>(assignments);
        this.assignments.add(assignment);
        return assignment;
    }

    public void closeCurrentAssignment(LocalDate endDate) {
        currentAssignment().ifPresent(current -> current.close(endDate));
    }

    // The client joined another trainer: the routine of the previous one stops applying and the
    // whole assignment history goes with the client.
    public void transferTo(TransferClientPlanCommand command) {
        closeCurrentAssignment(command.date());
        this.holderId = command.newHolderId();
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
