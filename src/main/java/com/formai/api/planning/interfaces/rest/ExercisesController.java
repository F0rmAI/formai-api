package com.formai.api.planning.interfaces.rest;

import com.formai.api.planning.domain.exceptions.ExerciseNotFoundException;
import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.commands.ArchiveExerciseCommand;
import com.formai.api.planning.domain.model.commands.DeleteExerciseCommand;
import com.formai.api.planning.domain.model.commands.RestoreExerciseCommand;
import com.formai.api.planning.domain.model.queries.GetExerciseByIdQuery;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.services.ExerciseCommandService;
import com.formai.api.planning.domain.services.ExerciseQueryService;
import com.formai.api.planning.interfaces.rest.resources.CreateExerciseResource;
import com.formai.api.planning.interfaces.rest.resources.ExercisePageResource;
import com.formai.api.planning.interfaces.rest.resources.ExerciseResource;
import com.formai.api.planning.interfaces.rest.transform.ExerciseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@Tag(name = "Exercises", description = "The trainer's own catalog of reusable exercises.")
@RestController
@RequestMapping("/api/v1/exercises")
public class ExercisesController {

    private final ExerciseCommandService exerciseCommandService;
    private final ExerciseQueryService exerciseQueryService;
    private final ExerciseAssembler assembler;

    public ExercisesController(ExerciseCommandService exerciseCommandService,
                               ExerciseQueryService exerciseQueryService,
                               ExerciseAssembler assembler) {
        this.exerciseCommandService = exerciseCommandService;
        this.exerciseQueryService = exerciseQueryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Add an exercise to my catalog",
            description = "Creates an active exercise with a name, a muscle group and optional equipment.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Exercise created",
                    content = @Content(schema = @Schema(implementation = ExerciseResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing name or muscle group, or too long",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "409", description = "An exercise with this name is already in the catalog",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<ExerciseResource> create(@Valid @RequestBody CreateExerciseResource resource,
                                                   Authentication authentication) {
        var command = assembler.toCommand(authentication.getName(), resource);
        return created(exerciseCommandService.handle(command));
    }

    @Operation(summary = "List my exercises",
            description = "The trainer's catalog sorted by name, optionally searched by name and filtered by status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of exercises",
                    content = @Content(schema = @Schema(implementation = ExercisePageResource.class))),
            @ApiResponse(responseCode = "400", description = "Unknown status, or an invalid page or size",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content)
    })
    @GetMapping
    public ResponseEntity<ExercisePageResource> getAll(@RequestParam(required = false) String search,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(defaultValue = "0") @Min(0) int page,
                                                       @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                       Authentication authentication) {
        var query = assembler.toQuery(authentication.getName(), search, status, page, size);
        return ResponseEntity.ok(assembler.toResource(exerciseQueryService.handle(query)));
    }

    @Operation(summary = "Get one of my exercises")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The exercise",
                    content = @Content(schema = @Schema(implementation = ExerciseResource.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such exercise in the catalog", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ExerciseResource> getById(@PathVariable UUID id, Authentication authentication) {
        var exercise = exerciseQueryService.handle(new GetExerciseByIdQuery(new ExerciseId(id), authentication.getName()))
                .orElseThrow(ExerciseNotFoundException::new);
        return ResponseEntity.ok(assembler.toResource(exercise));
    }

    @Operation(summary = "Delete an exercise",
            description = "Only an exercise that no routine uses can be deleted; otherwise archive it.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Exercise deleted"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such exercise in the catalog", content = @Content),
            @ApiResponse(responseCode = "409", description = "The exercise is used in a routine: archive it instead",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        exerciseCommandService.handle(new DeleteExerciseCommand(new ExerciseId(id), authentication.getName()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Archive an exercise",
            description = "Hides the exercise from new routines. Routines that already use it do not change.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Exercise archived",
                    content = @Content(schema = @Schema(implementation = ExerciseResource.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such exercise in the catalog", content = @Content)
    })
    @PostMapping("/{id}/archivals")
    public ResponseEntity<ExerciseResource> archive(@PathVariable UUID id, Authentication authentication) {
        return created(exerciseCommandService.handle(
                new ArchiveExerciseCommand(new ExerciseId(id), authentication.getName())));
    }

    @Operation(summary = "Restore an archived exercise",
            description = "Makes the exercise available for new routines again.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Exercise restored",
                    content = @Content(schema = @Schema(implementation = ExerciseResource.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such exercise in the catalog", content = @Content)
    })
    @PostMapping("/{id}/restorations")
    public ResponseEntity<ExerciseResource> restore(@PathVariable UUID id, Authentication authentication) {
        return created(exerciseCommandService.handle(
                new RestoreExerciseCommand(new ExerciseId(id), authentication.getName())));
    }

    private ResponseEntity<ExerciseResource> created(Optional<Exercise> exercise) {
        var saved = exercise.orElseThrow(() -> new IllegalStateException("An exercise command should never return empty"));
        return new ResponseEntity<>(assembler.toResource(saved), HttpStatus.CREATED);
    }
}
