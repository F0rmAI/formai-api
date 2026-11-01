package com.formai.api.planning.interfaces.rest.transform;

import com.formai.api.planning.domain.model.commands.AssignRoutineCommand;
import com.formai.api.planning.domain.model.commands.PersonalizeAssignmentCommand;
import com.formai.api.planning.domain.model.entities.Assignment;
import com.formai.api.planning.domain.model.valueobjects.AssignmentNote;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.PrescriptionAdjustment;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.TrainingDays;
import com.formai.api.planning.interfaces.rest.resources.AssignmentResource;
import com.formai.api.planning.interfaces.rest.resources.CreateAssignmentResource;
import com.formai.api.planning.interfaces.rest.resources.PrescriptionAdjustmentResource;
import com.formai.api.planning.interfaces.rest.resources.UpdateAssignmentResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ClientPlanAssembler {

    default AssignRoutineCommand toCommand(UUID routineId, UUID clientId, String holderId, LocalDate startDate,
                                           TrainingDays trainingDays, Optional<AssignmentNote> note) {
        return new AssignRoutineCommand(new RoutineId(routineId), new ClientId(clientId), holderId, startDate,
                trainingDays, note);
    }

    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "routineId", source = "assignment.routineId")
    @Mapping(target = "routineName", source = "routineName")
    @Mapping(target = "startDate", source = "assignment.period.startDate")
    @Mapping(target = "endDate", source = "assignment.period.endDate")
    @Mapping(target = "trainingDays", source = "assignment.trainingDays")
    @Mapping(target = "current", source = "assignment.current")
    @Mapping(target = "note", source = "assignment.note")
    @Mapping(target = "adjustments", source = "assignment.adjustments")
    AssignmentResource toResource(ClientId clientId, Assignment assignment, String routineName);

    PrescriptionAdjustmentResource toResource(PrescriptionAdjustment adjustment);

    default PersonalizeAssignmentCommand toCommand(UUID clientId, String holderId, UpdateAssignmentResource resource) {
        var adjustments = Optional.ofNullable(resource.adjustments()).orElse(List.of()).stream()
                .map(adjustment -> new PrescriptionAdjustment(adjustment.sessionOrder(),
                        new ExerciseId(adjustment.exerciseId()), adjustment.targetLoadKg(), adjustment.restSeconds()))
                .toList();
        return new PersonalizeAssignmentCommand(new ClientId(clientId), holderId, toNote(resource.note()), adjustments);
    }

    default Optional<AssignmentNote> toNote(String note) {
        return Optional.ofNullable(note).filter(value -> !value.isBlank()).map(AssignmentNote::new);
    }

    default String map(AssignmentNote note) {
        return note == null ? null : note.value();
    }

    default UUID map(ExerciseId id) {
        return id == null ? null : id.value();
    }

    default List<AssignRoutineCommand> toCommands(UUID routineId, String holderId, CreateAssignmentResource resource) {
        var trainingDays = toTrainingDays(resource.trainingDays());
        var note = toNote(resource.note());
        return resource.clientIds().stream()
                .distinct()
                .map(clientId -> toCommand(routineId, clientId, holderId, resource.startDate(), trainingDays, note))
                .toList();
    }

    default TrainingDays toTrainingDays(List<String> names) {
        try {
            return TrainingDays.ofNames(names);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    default List<String> map(TrainingDays trainingDays) {
        return trainingDays == null ? null : trainingDays.names();
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
