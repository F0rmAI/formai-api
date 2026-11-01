package com.formai.api.planning.interfaces.rest;

import com.formai.api.planning.domain.exceptions.ExerciseNotFoundException;
import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.commands.ArchiveExerciseCommand;
import com.formai.api.planning.domain.model.commands.DeleteExerciseCommand;
import com.formai.api.planning.domain.model.commands.RestoreExerciseCommand;
import com.formai.api.planning.domain.model.queries.GetExerciseByIdQuery;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.ExerciseSummary;
import com.formai.api.planning.domain.services.ExerciseCommandService;
import com.formai.api.planning.domain.services.ExerciseQueryService;
import com.formai.api.planning.interfaces.rest.resources.CreateExerciseResource;
import com.formai.api.planning.interfaces.rest.resources.ExercisePageResource;
import com.formai.api.planning.interfaces.rest.resources.ExerciseResource;
import com.formai.api.planning.interfaces.rest.resources.UpdateMachineLinkResource;
import com.formai.api.planning.interfaces.rest.transform.ExerciseAssembler;
import com.formai.api.shared.interfaces.rest.ApiTags;
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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@Tag(name = ApiTags.EXERCISES, description = ApiTags.EXERCISES_DESCRIPTION)
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
            description = "Creates an active exercise of my own with a name, a standard muscle group and optional "
                    + "equipment, when the name is in neither the global catalog nor mine.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Exercise created",
                    content = @Content(schema = @Schema(implementation = ExerciseResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing name or muscle group, or too long",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer", content = @Content),
            @ApiResponse(responseCode = "409", description = "An exercise with this name is already in the global "
                    + "catalog or in mine", content = @Content),
            @ApiResponse(responseCode = "422", description = "The muscle group is not a standard one",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<ExerciseResource> create(@Valid @RequestBody CreateExerciseResource resource,
                                                   Authentication authentication) {
        var command = assembler.toCommand(authentication.getName(), resource);
        return created(exerciseCommandService.handle(command), authentication);
    }

    @Operation(summary = "List my exercises",
            description = "The global catalog plus the trainer's own exercises, sorted by name, optionally searched "
                    + "by name and filtered by muscle group, origin (GLOBAL or OWN) and status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of exercises",
                    content = @Content(schema = @Schema(implementation = ExercisePageResource.class))),
            @ApiResponse(responseCode = "400", description = "Unknown origin or status, or an invalid page or size",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer", content = @Content)
    })
    @GetMapping
    public ResponseEntity<ExercisePageResource> getAll(@RequestParam(required = false) String search,
                                                       @RequestParam(required = false) String muscleGroup,
                                                       @RequestParam(required = false) String origin,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(defaultValue = "0") @Min(0) int page,
                                                       @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                       Authentication authentication) {
        var query = assembler.toQuery(authentication.getName(), search, muscleGroup, origin, status, page, size);
        return ResponseEntity.ok(assembler.toResource(exerciseQueryService.handle(query)));
    }

    @Operation(summary = "Get one of my exercises")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The exercise",
                    content = @Content(schema = @Schema(implementation = ExerciseResource.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such exercise in the catalog", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ExerciseResource> getById(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(assembler.toResource(summaryOf(id, authentication.getName())));
    }

    @Operation(summary = "Delete an exercise",
            description = "Only an exercise that no routine uses can be deleted; otherwise archive it.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Exercise deleted"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer", content = @Content),
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
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such exercise in the catalog", content = @Content)
    })
    @PostMapping("/{id}/archivals")
    public ResponseEntity<ExerciseResource> archive(@PathVariable UUID id, Authentication authentication) {
        return created(exerciseCommandService.handle(
                new ArchiveExerciseCommand(new ExerciseId(id), authentication.getName())), authentication);
    }

    @Operation(summary = "Restore an archived exercise",
            description = "Makes the exercise available for new routines again.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Exercise restored",
                    content = @Content(schema = @Schema(implementation = ExerciseResource.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such exercise in the catalog", content = @Content)
    })
    @PostMapping("/{id}/restorations")
    public ResponseEntity<ExerciseResource> restore(@PathVariable UUID id, Authentication authentication) {
        return created(exerciseCommandService.handle(
                new RestoreExerciseCommand(new ExerciseId(id), authentication.getName())), authentication);
    }

    @Operation(summary = "Link an exercise to a machine",
            description = "Links an exercise of my catalog to a published machine, so its usage guide goes with the "
                    + "exercise in my clients' routines. No machine is published until the machine catalog exists.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exercise linked to the machine",
                    content = @Content(schema = @Schema(implementation = ExerciseResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing machine id", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such exercise in the catalog", content = @Content),
            @ApiResponse(responseCode = "422", description = "The machine is not published",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}/machine-link")
    public ResponseEntity<ExerciseResource> linkMachine(@PathVariable UUID id,
                                                        @Valid @RequestBody UpdateMachineLinkResource resource,
                                                        Authentication authentication) {
        var exercise = exerciseCommandService.handle(assembler.toCommand(id, authentication.getName(), resource))
                .orElseThrow(() -> new IllegalStateException("An exercise command should never return empty"));
        return ResponseEntity.ok(assembler.toResource(summaryOf(exercise.getId().value(), authentication.getName())));
    }

    private ResponseEntity<ExerciseResource> created(Optional<Exercise> exercise, Authentication authentication) {
        var saved = exercise.orElseThrow(() -> new IllegalStateException("An exercise command should never return empty"));
        return new ResponseEntity<>(assembler.toResource(summaryOf(saved.getId().value(), authentication.getName())),
                HttpStatus.CREATED);
    }

    private ExerciseSummary summaryOf(UUID id, String holderId) {
        return exerciseQueryService.handle(new GetExerciseByIdQuery(new ExerciseId(id), holderId))
                .orElseThrow(ExerciseNotFoundException::new);
    }
}
