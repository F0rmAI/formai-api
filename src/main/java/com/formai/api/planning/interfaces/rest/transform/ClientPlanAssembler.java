package com.formai.api.planning.interfaces.rest.transform;

import com.formai.api.planning.domain.model.commands.AssignRoutineCommand;
import com.formai.api.planning.domain.model.entities.Assignment;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.interfaces.rest.resources.AssignmentResource;
import com.formai.api.planning.interfaces.rest.resources.CreateAssignmentResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ClientPlanAssembler {

    AssignRoutineCommand toCommand(UUID routineId, UUID clientId, String holderId, LocalDate startDate);

    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "routineId", source = "assignment.routineId")
    @Mapping(target = "routineName", source = "routineName")
    @Mapping(target = "startDate", source = "assignment.period.startDate")
    @Mapping(target = "endDate", source = "assignment.period.endDate")
    @Mapping(target = "current", source = "assignment.current")
    AssignmentResource toResource(ClientId clientId, Assignment assignment, String routineName);

    default List<AssignRoutineCommand> toCommands(UUID routineId, String holderId, CreateAssignmentResource resource) {
        return resource.clientIds().stream()
                .distinct()
                .map(clientId -> toCommand(routineId, clientId, holderId, resource.startDate()))
                .toList();
    }

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(RoutineId id) {
        return id == null ? null : id.value();
    }

    default RoutineId mapRoutineId(UUID value) {
        return value == null ? null : new RoutineId(value);
    }

    default UUID map(ClientId id) {
        return id == null ? null : id.value();
    }

    default ClientId mapClientId(UUID value) {
        return value == null ? null : new ClientId(value);
    }
}
