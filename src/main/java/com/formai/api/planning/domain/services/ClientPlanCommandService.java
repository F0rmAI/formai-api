package com.formai.api.planning.domain.services;

import com.formai.api.planning.domain.model.aggregates.ClientPlan;
import com.formai.api.planning.domain.model.commands.AssignRoutineCommand;
import com.formai.api.planning.domain.model.commands.CloseAssignmentCommand;
import com.formai.api.planning.domain.model.commands.TransferClientPlanCommand;

import java.util.Optional;

public interface ClientPlanCommandService {

    Optional<ClientPlan> handle(AssignRoutineCommand command);

    void handle(CloseAssignmentCommand command);

    void handle(TransferClientPlanCommand command);
}
