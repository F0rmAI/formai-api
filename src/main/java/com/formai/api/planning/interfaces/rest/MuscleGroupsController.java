package com.formai.api.planning.interfaces.rest;

import com.formai.api.planning.domain.model.queries.GetMuscleGroupsQuery;
import com.formai.api.planning.domain.model.valueobjects.MuscleGroup;
import com.formai.api.planning.domain.services.ExerciseQueryService;
import com.formai.api.shared.interfaces.rest.ApiTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = ApiTags.EXERCISES, description = ApiTags.EXERCISES_DESCRIPTION)
@RestController
@RequestMapping("/api/v1/muscle-groups")
public class MuscleGroupsController {

    private final ExerciseQueryService exerciseQueryService;

    public MuscleGroupsController(ExerciseQueryService exerciseQueryService) {
        this.exerciseQueryService = exerciseQueryService;
    }

    @Operation(summary = "List the muscle groups",
            description = "The standard muscle groups an exercise is filed under, in display order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The muscle groups",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<String>> getAll() {
        return ResponseEntity.ok(exerciseQueryService.handle(new GetMuscleGroupsQuery()).stream()
                .map(MuscleGroup::value).toList());
    }
}
