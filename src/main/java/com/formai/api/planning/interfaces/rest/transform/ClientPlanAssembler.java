package com.formai.api.planning.interfaces.rest.transform;

import com.formai.api.planning.domain.model.commands.AssignRoutineCommand;
import com.formai.api.planning.domain.model.entities.Assignment;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.interfaces.rest.resources.AssignmentResource;
import com.formai.api.planning.interfaces.rest.resources.CreateAssignmentResource;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ClientPlanAssembler {

    // One command per client, without repeating a client listed twice.
    default List<AssignRoutineCommand> toCommands(UUID routineId, String holderId, CreateAssignmentResource resource) {
        return resource.clientIds().stream()
                .distinct()
                .map(clientId -> new AssignRoutineCommand(new RoutineId(routineId), new ClientId(clientId), holderId,
                        resource.startDate()))
                .toList();
    }

    default AssignmentResource toResource(ClientId clientId, Assignment assignment, String routineName) {
        return new AssignmentResource(clientId.value(), assignment.getRoutineId().value(), routineName,
                assignment.getPeriod().startDate(), assignment.getPeriod().endDate(), assignment.isCurrent());
    }
}
