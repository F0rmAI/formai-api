package com.formai.api.tracking.interfaces.rest;

import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.services.WorkoutSessionQueryService;
import com.formai.api.tracking.interfaces.rest.resources.ProgressChartResource;
import com.formai.api.tracking.interfaces.rest.transform.WorkoutSessionAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Composition controller: /clients/{id}/progress-charts borrows the namespace of the clients module's
// Client aggregate. It lives in tracking because tracking already depends on clients: serving this
// route from clients would make clients depend on tracking and close a cycle. It holds no
// business logic; the rules live in the tracking domain (ProgressCalculator).
@Tag(name = "Progress charts", description = "How an exercise's heaviest load and volume evolved over 4, 8 or 12 weeks.")
@RestController
public class ProgressChartsController {

    private final WorkoutSessionQueryService workoutSessionQueryService;
    private final WorkoutSessionAssembler assembler;

    public ProgressChartsController(WorkoutSessionQueryService workoutSessionQueryService,
                                    WorkoutSessionAssembler assembler) {
        this.workoutSessionQueryService = workoutSessionQueryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Get my progress chart for an exercise",
            description = "One point per session in which the exercise was recorded, oldest first. "
                    + "With fewer than two sessions, enoughData=false.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The chart points",
                    content = @Content(schema = @Schema(implementation = ProgressChartResource.class))),
            @ApiResponse(responseCode = "400", description = "weeks is not 4, 8 or 12", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a client",
                    content = @Content)
    })
    @GetMapping("/api/v1/progress-charts/me")
    public ResponseEntity<ProgressChartResource> getMine(@RequestParam UUID exerciseId,
                                                         @RequestParam(defaultValue = "4") int weeks,
                                                         Authentication authentication) {
        var clientId = assembler.toClientId(authentication.getName());
        var query = assembler.toExerciseProgressQuery(clientId, authentication.getName(), exerciseId, weeks);
        return ResponseEntity.ok(assembler.toResource(workoutSessionQueryService.handle(query)));
    }

    @Operation(summary = "Get a client's progress chart for an exercise",
            description = "The same chart for one of the trainer's clients.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The chart points",
                    content = @Content(schema = @Schema(implementation = ProgressChartResource.class))),
            @ApiResponse(responseCode = "400", description = "weeks is not 4, 8 or 12", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, not a trainer, or not "
                    + "one of your clients", content = @Content)
    })
    @GetMapping("/api/v1/clients/{id}/progress-charts")
    public ResponseEntity<ProgressChartResource> getByClient(@PathVariable UUID id,
                                                             @RequestParam UUID exerciseId,
                                                             @RequestParam(defaultValue = "4") int weeks,
                                                             Authentication authentication) {
        var query = assembler.toExerciseProgressQuery(new ClientId(id), authentication.getName(), exerciseId, weeks);
        return ResponseEntity.ok(assembler.toResource(workoutSessionQueryService.handle(query)));
    }
}
