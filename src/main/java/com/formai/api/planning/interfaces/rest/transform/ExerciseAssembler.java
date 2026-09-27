package com.formai.api.planning.interfaces.rest.transform;

import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.commands.CreateExerciseCommand;
import com.formai.api.planning.domain.model.queries.GetExercisesQuery;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.ExerciseName;
import com.formai.api.planning.domain.model.valueobjects.ExercisePage;
import com.formai.api.planning.domain.model.valueobjects.ExerciseStatus;
import com.formai.api.planning.domain.model.valueobjects.MuscleGroup;
import com.formai.api.planning.domain.model.valueobjects.Pagination;
import com.formai.api.planning.interfaces.rest.resources.CreateExerciseResource;
import com.formai.api.planning.interfaces.rest.resources.ExercisePageResource;
import com.formai.api.planning.interfaces.rest.resources.ExerciseResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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

    default CreateExerciseCommand toCommand(String holderId, CreateExerciseResource resource) {
        return new CreateExerciseCommand(holderId, new ExerciseName(resource.name()),
                new MuscleGroup(resource.muscleGroup()), Optional.ofNullable(resource.equipment()));
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

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(ExerciseId id) {
        return id == null ? null : id.value();
    }

    default String map(ExerciseName name) {
        return name == null ? null : name.value();
    }

    default String map(MuscleGroup muscleGroup) {
        return muscleGroup == null ? null : muscleGroup.value();
    }
}
