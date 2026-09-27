package com.formai.api.tracking.interfaces.rest;

import com.formai.api.tracking.domain.exceptions.WorkoutSessionNotFoundException;
import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.queries.GetWorkoutSessionByIdQuery;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionId;
import com.formai.api.tracking.domain.services.WorkoutSessionCommandService;
import com.formai.api.tracking.domain.services.WorkoutSessionQueryService;
import com.formai.api.tracking.interfaces.rest.resources.CreateSessionCompletionResource;
import com.formai.api.tracking.interfaces.rest.resources.CreateSetCorrectionResource;
import com.formai.api.tracking.interfaces.rest.resources.CreateSetResource;
import com.formai.api.tracking.interfaces.rest.resources.WorkoutSessionPageResource;
import com.formai.api.tracking.interfaces.rest.resources.WorkoutSessionResource;
import com.formai.api.tracking.interfaces.rest.transform.WorkoutSessionAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Workouts", description = "The client's active routine, today's session, set recording, " +
        "session completion and workout history.")
@RestController
@RequestMapping("/api/v1")
public class WorkoutSessionsController {

    private final WorkoutSessionCommandService workoutSessionCommandService;
    private final WorkoutSessionQueryService workoutSessionQueryService;
    private final WorkoutSessionAssembler assembler;

    public WorkoutSessionsController(WorkoutSessionCommandService workoutSessionCommandService,
                                     WorkoutSessionQueryService workoutSessionQueryService,
                                     WorkoutSessionAssembler assembler) {
        this.workoutSessionCommandService = workoutSessionCommandService;
        this.workoutSessionQueryService = workoutSessionQueryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Get my workout history",
            description = "The signed-in client's sessions, most recent first, each with its total volume " +
                    "and the load and repetitions of every set. Optionally filtered by a date range.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of sessions",
                    content = @Content(schema = @Schema(implementation = WorkoutSessionPageResource.class))),
            @ApiResponse(responseCode = "400", description = "Only one of 'from'/'to', 'from' after 'to', " +
                    "or an invalid page or size", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content)
    })
    @GetMapping("/workout-sessions")
    public ResponseEntity<WorkoutSessionPageResource> getHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication) {
        var holderId = authentication.getName();
        var query = assembler.toHistoryQuery(assembler.toClientId(holderId), holderId, from, to, page, size);
        return ResponseEntity.ok(assembler.toResource(workoutSessionQueryService.handle(query)));
    }

    @Operation(summary = "Get one of my workout sessions",
            description = "A session of the signed-in client with the prescription and the recorded sets.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The session",
                    content = @Content(schema = @Schema(implementation = WorkoutSessionResource.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such session for this client", content = @Content)
    })
    @GetMapping("/workout-sessions/{id}")
    public ResponseEntity<WorkoutSessionResource> getById(@PathVariable UUID id, Authentication authentication) {
        var holderId = authentication.getName();
        var query = new GetWorkoutSessionByIdQuery(new WorkoutSessionId(id), assembler.toClientId(holderId), holderId);
        var session = workoutSessionQueryService.handle(query).orElseThrow(WorkoutSessionNotFoundException::new);
        return ResponseEntity.ok(assembler.toResource(session));
    }

    @Operation(summary = "Record a set",
            description = "Records the load and repetitions of one set of an exercise in a pending session. " +
                    "Recording a set number again replaces it instead of duplicating it.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Set recorded — returns the updated session",
                    content = @Content(schema = @Schema(implementation = WorkoutSessionResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing exercise, set number, load or reps",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such session for this client", content = @Content),
            @ApiResponse(responseCode = "409", description = "The session is already finished or skipped",
                    content = @Content),
            @ApiResponse(responseCode = "422", description = "Negative load or reps, a set number outside the " +
                    "prescription, or an exercise that is not in the session", content = @Content)
    })
    @PostMapping("/workout-sessions/{id}/sets")
    public ResponseEntity<WorkoutSessionResource> recordSet(@PathVariable UUID id,
                                                            @Valid @RequestBody CreateSetResource resource,
                                                            Authentication authentication) {
        var command = assembler.toCommand(id, authentication.getName(), resource);
        return created(workoutSessionCommandService.handle(command));
    }

    @Operation(summary = "Correct a recorded set",
            description = "Replaces the load and repetitions of a set already recorded, without duplicating it.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Set corrected — returns the updated session",
                    content = @Content(schema = @Schema(implementation = WorkoutSessionResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing exercise, set number, load or reps",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such session for this client", content = @Content),
            @ApiResponse(responseCode = "409", description = "The session is already finished or skipped",
                    content = @Content),
            @ApiResponse(responseCode = "422", description = "Negative load or reps, or a set not recorded yet",
                    content = @Content)
    })
    @PostMapping("/workout-sessions/{id}/corrections")
    public ResponseEntity<WorkoutSessionResource> correctSet(@PathVariable UUID id,
                                                             @Valid @RequestBody CreateSetCorrectionResource resource,
                                                             Authentication authentication) {
        var command = assembler.toCommand(id, authentication.getName(), resource);
        return created(workoutSessionCommandService.handle(command));
    }

    @Operation(summary = "Finish a session",
            description = "Closes the session and returns its compliance status: COMPLETED when every " +
                    "exercise has sets recorded, PARTIAL when some do not and confirmPartial is true.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Session finished — returns it with its status",
                    content = @Content(schema = @Schema(implementation = WorkoutSessionResource.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such session for this client", content = @Content),
            @ApiResponse(responseCode = "409", description = "Already finished or skipped, or some exercises " +
                    "have no sets and confirmPartial is false — ask the client to confirm", content = @Content)
    })
    @PostMapping("/workout-sessions/{id}/completions")
    public ResponseEntity<WorkoutSessionResource> finish(@PathVariable UUID id,
                                                         @RequestBody CreateSessionCompletionResource resource,
                                                         Authentication authentication) {
        var command = assembler.toCommand(id, authentication.getName(), resource);
        return created(workoutSessionCommandService.handle(command));
    }

    // Served by tracking under the clients' path: clients never depends on tracking.
    @Operation(summary = "Get a client's workout history",
            description = "For trainers: the sessions recorded by one of their clients, most recent first, " +
                    "with the load and repetitions of every set. Optionally filtered by a date range.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of sessions",
                    content = @Content(schema = @Schema(implementation = WorkoutSessionPageResource.class))),
            @ApiResponse(responseCode = "400", description = "Only one of 'from'/'to', 'from' after 'to', " +
                    "or an invalid page or size", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not one of " +
                    "the signed-in trainer's clients", content = @Content)
    })
    @GetMapping("/clients/{id}/workout-sessions")
    public ResponseEntity<WorkoutSessionPageResource> getHistoryOfClient(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication) {
        var query = assembler.toHistoryQuery(new ClientId(id), authentication.getName(), from, to, page, size);
        return ResponseEntity.ok(assembler.toResource(workoutSessionQueryService.handle(query)));
    }

    private ResponseEntity<WorkoutSessionResource> created(Optional<WorkoutSession> session) {
        var saved = session.orElseThrow(() -> new IllegalStateException("A session command should never return empty"));
        return new ResponseEntity<>(assembler.toResource(saved), HttpStatus.CREATED);
    }
}
