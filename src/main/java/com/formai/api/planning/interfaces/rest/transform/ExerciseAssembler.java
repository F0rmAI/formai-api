package com.formai.api.planning.interfaces.rest.transform;

import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.commands.CreateExerciseCommand;
import com.formai.api.planning.domain.model.commands.LinkExerciseToMachineCommand;
import com.formai.api.planning.domain.model.queries.GetExercisesQuery;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.ExerciseName;
import com.formai.api.planning.domain.model.valueobjects.ExercisePage;
import com.formai.api.planning.domain.model.valueobjects.ExerciseStatus;
import com.formai.api.planning.domain.model.valueobjects.MachineId;
import com.formai.api.planning.domain.model.valueobjects.MuscleGroup;
import com.formai.api.planning.domain.model.valueobjects.Pagination;
import com.formai.api.planning.interfaces.rest.resources.CreateExerciseResource;
import com.formai.api.planning.interfaces.rest.resources.ExercisePageResource;
import com.formai.api.planning.interfaces.rest.resources.ExerciseResource;
import com.formai.api.planning.interfaces.rest.resources.UpdateMachineLinkResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ExerciseAssembler {

    ExerciseResource toResource(Exercise exercise);

    @Mapping(target = "content", source = "items")
    ExercisePageResource toResource(ExercisePage page);

    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "name", source = "resource.name")
    @Mapping(target = "muscleGroup", source = "resource.muscleGroup")
    @Mapping(target = "equipment", source = "resource.equipment", qualifiedByName = "optionalText")
    CreateExerciseCommand toCommand(String holderId, CreateExerciseResource resource);

    default LinkExerciseToMachineCommand toCommand(UUID exerciseId, String holderId,
                                                   UpdateMachineLinkResource resource) {
        return new LinkExerciseToMachineCommand(new ExerciseId(exerciseId), holderId, new MachineId(resource.machineId()));
    }

    default GetExercisesQuery toQuery(String holderId, String search, String status, int page, int size) {
        return new GetExercisesQuery(holderId, Optional.ofNullable(search).filter(value -> !value.isBlank()),
                toStatus(status), new Pagination(page, size));
    }

    default Optional<ExerciseStatus> toStatus(String status) {
        if (status == null || status.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(ExerciseStatus.values())
                .filter(value -> value.name().equalsIgnoreCase(status))
                .findFirst()
                .map(Optional::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Status must be ACTIVE or ARCHIVED"));
    }

    @Named("optionalText")
    default Optional<String> optionalText(String value) {
        return Optional.ofNullable(value);
    }

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(ExerciseId id) {
        return id == null ? null : id.value();
    }

    default UUID map(MachineId machineId) {
        return machineId == null ? null : machineId.value();
    }

    default String map(ExerciseName name) {
        return name == null ? null : name.value();
    }

    default ExerciseName mapExerciseName(String value) {
        return value == null ? null : new ExerciseName(value);
    }

    default String map(MuscleGroup muscleGroup) {
        return muscleGroup == null ? null : muscleGroup.value();
    }

    default MuscleGroup mapMuscleGroup(String value) {
        return value == null ? null : new MuscleGroup(value);
    }
}
