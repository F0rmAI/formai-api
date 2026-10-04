package com.formai.api.planning.interfaces.rest;

import com.formai.api.planning.domain.exceptions.RoutineNotFoundException;
import com.formai.api.planning.domain.model.aggregates.Routine;
import com.formai.api.planning.domain.model.queries.GetRoutineByIdQuery;
import com.formai.api.planning.domain.model.queries.GetRoutineVersionsQuery;
import com.formai.api.planning.domain.model.queries.GetRoutinesQuery;
import com.formai.api.planning.domain.model.valueobjects.Pagination;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.services.ClientPlanCommandService;
import com.formai.api.planning.domain.services.RoutineCommandService;
import com.formai.api.planning.domain.services.RoutineQueryService;
import com.formai.api.planning.interfaces.rest.resources.AssignmentResource;
import com.formai.api.planning.interfaces.rest.resources.CreateAssignmentResource;
import com.formai.api.planning.interfaces.rest.resources.CreateRoutineDuplicateResource;
import com.formai.api.planning.interfaces.rest.resources.CreateRoutineResource;
import com.formai.api.planning.interfaces.rest.resources.RoutinePageResource;
import com.formai.api.planning.interfaces.rest.resources.RoutineResource;
import com.formai.api.planning.interfaces.rest.resources.RoutineVersionResource;
import com.formai.api.planning.interfaces.rest.resources.UpdateRoutineResource;
import com.formai.api.planning.interfaces.rest.transform.ClientPlanAssembler;
import com.formai.api.planning.interfaces.rest.transform.RoutineAssembler;
import com.formai.api.shared.interfaces.rest.ApiTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Tag(name = ApiTags.ROUTINES, description = ApiTags.ROUTINES_DESCRIPTION)
@RestController
@RequestMapping("/api/v1/routines")
public class RoutinesController {

    private final RoutineCommandService routineCommandService;
    private final RoutineQueryService routineQueryService;
    private final ClientPlanCommandService clientPlanCommandService;
    private final RoutineAssembler assembler;
    private final ClientPlanAssembler clientPlanAssembler;

    public RoutinesController(RoutineCommandService routineCommandService,
                              RoutineQueryService routineQueryService,
                              ClientPlanCommandService clientPlanCommandService,
                              RoutineAssembler assembler,
                              ClientPlanAssembler clientPlanAssembler) {
        this.routineCommandService = routineCommandService;
        this.routineQueryService = routineQueryService;
        this.clientPlanCommandService = clientPlanCommandService;
        this.assembler = assembler;
        this.clientPlanAssembler = clientPlanAssembler;
    }

    @Operation(summary = "Create a routine",
            description = "Creates a DRAFT routine with its sessions and, for every exercise of the catalog, its " +
                    "sets, reps, target load and rest. It becomes version 1.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Routine created",
                    content = @Content(schema = @Schema(implementation = RoutineResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing name, sessions or prescription values",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer", content = @Content),
            @ApiResponse(responseCode = "404", description = "An exercise is not in the catalog", content = @Content),
            @ApiResponse(responseCode = "422", description = "No sessions, a session without exercises, sets or " +
                    "reps of zero or less, a negative load or rest, or an archived exercise", content = @Content)
    })
    @PostMapping
    public ResponseEntity<RoutineResource> create(@Valid @RequestBody CreateRoutineResource resource,
                                                  Authentication authentication) {
        var command = assembler.toCommand(authentication.getName(), resource);
        return withStatus(routineCommandService.handle(command), HttpStatus.CREATED);
    }

    @Operation(summary = "List my routines", description = "The trainer's routines, most recent first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of routines",
                    content = @Content(schema = @Schema(implementation = RoutinePageResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid page or size", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer", content = @Content)
    })
    @GetMapping
    public ResponseEntity<RoutinePageResource> getAll(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                      Authentication authentication) {
        var query = new GetRoutinesQuery(authentication.getName(), new Pagination(page, size));
        return ResponseEntity.ok(assembler.toResource(routineQueryService.handle(query)));
    }

    @Operation(summary = "Get one of my routines", description = "The routine at its current version.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The routine",
                    content = @Content(schema = @Schema(implementation = RoutineResource.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such routine", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<RoutineResource> getById(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(assembler.toResource(findOwnRoutine(id, authentication.getName())));
    }

    @Operation(summary = "Modify a routine",
            description = "Saves the change as a new version with its date and author; earlier versions are kept. " +
                    "Clients following the routine see the new version, and workouts already recorded keep the " +
                    "version they were done with.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Routine updated to a new version",
                    content = @Content(schema = @Schema(implementation = RoutineResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing name, sessions or prescription values",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such routine, or an exercise not in the catalog",
                    content = @Content),
            @ApiResponse(responseCode = "422", description = "No sessions, a session without exercises, sets or " +
                    "reps of zero or less, a negative load or rest, or an archived exercise", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<RoutineResource> update(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateRoutineResource resource,
                                                  Authentication authentication) {
        var command = assembler.toCommand(id, authentication.getName(), resource);
        return withStatus(routineCommandService.handle(command), HttpStatus.OK);
    }

    @Operation(summary = "Get a routine's version history", description = "Every version, most recent first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The versions",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoutineVersionResource.class)))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such routine", content = @Content)
    })
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<RoutineVersionResource>> getVersions(@PathVariable UUID id,
                                                                    Authentication authentication) {
        var versions = routineQueryService.handle(new GetRoutineVersionsQuery(new RoutineId(id),
                authentication.getName()));
        if (versions.isEmpty()) {
            throw new RoutineNotFoundException();
        }
        return ResponseEntity.ok(assembler.toVersionResources(versions));
    }

    @Operation(summary = "Duplicate a routine",
            description = "Creates an editable DRAFT copy of the routine's current version, with no clients assigned.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Copy created",
                    content = @Content(schema = @Schema(implementation = RoutineResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing name", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such routine", content = @Content)
    })
    @PostMapping("/{id}/duplicates")
    public ResponseEntity<RoutineResource> duplicate(@PathVariable UUID id,
                                                     @Valid @RequestBody CreateRoutineDuplicateResource resource,
                                                     Authentication authentication) {
        var command = assembler.toCommand(id, authentication.getName(), resource);
        return withStatus(routineCommandService.handle(command), HttpStatus.CREATED);
    }

    @Operation(summary = "Assign a routine to clients",
            description = "Assigns the routine from the start date to each client listed, closing the routine " +
                    "each of them had until then. trainingDays are the days of the week they train (MONDAY … " +
                    "SUNDAY; every day when omitted): a session is scheduled only on those days. Clients are " +
                    "assigned one by one: if one fails, those before it stay assigned and the response explains " +
                    "the failure.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "One current assignment per client",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AssignmentResource.class)))),
            @ApiResponse(responseCode = "400", description = "No clients, no start date, or an unknown training day",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such routine, or a client that is not yours",
                    content = @Content),
            @ApiResponse(responseCode = "422", description = "A client is not active, or the number of training " +
                    "days differs from the number of sessions of the routine", content = @Content)
    })
    @PostMapping("/{id}/assignments")
    public ResponseEntity<List<AssignmentResource>> assign(@PathVariable UUID id,
                                                           @Valid @RequestBody CreateAssignmentResource resource,
                                                           Authentication authentication) {
        var holderId = authentication.getName();
        var routineName = findOwnRoutine(id, holderId).getName().value();
        var assignments = clientPlanAssembler.toCommands(id, holderId, resource).stream()
                .map(command -> {
                    var plan = clientPlanCommandService.handle(command)
                            .orElseThrow(() -> new IllegalStateException("Assigning should never return empty"));
                    var current = plan.currentAssignment()
                            .orElseThrow(() -> new IllegalStateException("A new assignment is always current"));
                    return clientPlanAssembler.toResource(plan.getClientId(), current, routineName);
                })
                .toList();
        return new ResponseEntity<>(assignments, HttpStatus.CREATED);
    }

    private Routine findOwnRoutine(UUID id, String holderId) {
        return routineQueryService.handle(new GetRoutineByIdQuery(new RoutineId(id), holderId))
                .orElseThrow(RoutineNotFoundException::new);
    }

    private ResponseEntity<RoutineResource> withStatus(Optional<Routine> routine, HttpStatus status) {
        var saved = routine.orElseThrow(() -> new IllegalStateException("A routine command should never return empty"));
        return new ResponseEntity<>(assembler.toResource(saved), status);
    }
}
