package com.formai.api.planning.interfaces.rest;

import com.formai.api.planning.domain.model.aggregates.Routine;
import com.formai.api.planning.domain.model.entities.Assignment;
import com.formai.api.planning.domain.model.queries.GetClientPlanQuery;
import com.formai.api.planning.domain.model.queries.GetRoutineByIdQuery;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.RoutineName;
import com.formai.api.planning.domain.services.ClientPlanCommandService;
import com.formai.api.planning.domain.services.ClientPlanQueryService;
import com.formai.api.planning.domain.services.RoutineQueryService;
import com.formai.api.planning.interfaces.rest.resources.AssignmentResource;
import com.formai.api.planning.interfaces.rest.resources.UpdateAssignmentResource;
import com.formai.api.planning.interfaces.rest.transform.ClientPlanAssembler;
import com.formai.api.shared.interfaces.rest.ApiTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

// Composition controller: /clients/{id}/assignments borrows the namespace of the clients module's
// Client aggregate. It lives in planning because planning already depends on clients: serving this
// route from clients would make clients depend on planning and close a cycle. It holds no
// business logic; the rules live in the planning domain.
@Tag(name = ApiTags.ROUTINES, description = ApiTags.ROUTINES_DESCRIPTION)
@RestController
@RequestMapping("/api/v1/clients/{id}/assignments")
public class AssignmentsController {

    private final ClientPlanCommandService clientPlanCommandService;
    private final ClientPlanQueryService clientPlanQueryService;
    private final RoutineQueryService routineQueryService;
    private final ClientPlanAssembler assembler;

    public AssignmentsController(ClientPlanCommandService clientPlanCommandService,
                                 ClientPlanQueryService clientPlanQueryService,
                                 RoutineQueryService routineQueryService,
                                 ClientPlanAssembler assembler) {
        this.clientPlanCommandService = clientPlanCommandService;
        this.clientPlanQueryService = clientPlanQueryService;
        this.routineQueryService = routineQueryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Get a client's assignment history",
            description = "Every routine assigned to one of my clients, most recent first; the current one has no " +
                    "end date. Empty when the client has none.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The assignments",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AssignmentResource.class)))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, not a trainer, or not " +
                    "one of your clients", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<AssignmentResource>> getByClient(@PathVariable UUID id, Authentication authentication) {
        var holderId = authentication.getName();
        var clientId = new ClientId(id);
        var routineNames = new HashMap<RoutineId, String>();
        var assignments = clientPlanQueryService.handle(new GetClientPlanQuery(clientId, holderId))
                .map(plan -> plan.getAssignments().stream()
                        .sorted(Comparator.comparing((Assignment assignment) -> assignment.getPeriod().startDate())
                                .reversed())
                        .map(assignment -> assembler.toResource(clientId, assignment,
                                routineNames.computeIfAbsent(assignment.getRoutineId(),
                                        routineId -> routineName(routineId, holderId))))
                        .toList())
                .orElse(List.of());
        return ResponseEntity.ok(assignments);
    }

    @Operation(summary = "Personalize a client's current assignment",
            description = "Replaces the trainer's note and the client's own target load and rest per exercise of a " +
                    "session, without changing the routine other clients may follow. A blank note removes it and " +
                    "an empty list removes every adjustment. The client sees them on the next sync of the app.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The personalized current assignment",
                    content = @Content(schema = @Schema(implementation = AssignmentResource.class))),
            @ApiResponse(responseCode = "400", description = "Note too long or an adjustment without its exercise",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, not a trainer, or not " +
                    "one of your clients", content = @Content),
            @ApiResponse(responseCode = "404", description = "The client has no current assignment",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "An adjustment with negative values, with nothing to " +
                    "change, repeated, or for an exercise the session does not prescribe",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/current")
    public ResponseEntity<AssignmentResource> updateCurrent(@PathVariable UUID id,
                                                            @Valid @RequestBody UpdateAssignmentResource resource,
                                                            Authentication authentication) {
        var holderId = authentication.getName();
        var plan = clientPlanCommandService.handle(assembler.toCommand(id, holderId, resource))
                .orElseThrow(() -> new IllegalStateException("Personalizing should never return empty"));
        var current = plan.currentAssignment()
                .orElseThrow(() -> new IllegalStateException("A personalized assignment is always current"));
        return ResponseEntity.ok(assembler.toResource(plan.getClientId(), current,
                routineName(current.getRoutineId(), holderId)));
    }

    private String routineName(RoutineId routineId, String holderId) {
        return routineQueryService.handle(new GetRoutineByIdQuery(routineId, holderId))
                .map(Routine::getName)
                .map(RoutineName::value)
                .orElse(null);
    }
}
