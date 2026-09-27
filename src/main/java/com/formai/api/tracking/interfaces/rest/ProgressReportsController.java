package com.formai.api.tracking.interfaces.rest;

import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.services.WorkoutSessionQueryService;
import com.formai.api.tracking.interfaces.rest.resources.ProgressReportResource;
import com.formai.api.tracking.interfaces.rest.transform.WorkoutSessionAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

// Composition controller: /clients/{id}/progress-reports borrows the namespace of the clients module's
// Client aggregate. It lives in tracking because tracking already depends on clients: serving this
// route from clients would make clients depend on tracking and close a cycle. It holds no
// business logic; the rules live in the tracking domain (ProgressCalculator).
@Tag(name = "Progress reports", description = "A client's adherence and exercise progress over a period.")
@RestController
public class ProgressReportsController {

    private final WorkoutSessionQueryService workoutSessionQueryService;
    private final WorkoutSessionAssembler assembler;

    public ProgressReportsController(WorkoutSessionQueryService workoutSessionQueryService,
                                     WorkoutSessionAssembler assembler) {
        this.workoutSessionQueryService = workoutSessionQueryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Get a client's progress over a period",
            description = "Adherence (completed ÷ scheduled × 100), sessions by status, and each exercise's heaviest "
                    + "load and volume in its first and last session. Without records: 0 % and hasData=false.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The progress report",
                    content = @Content(schema = @Schema(implementation = ProgressReportResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing dates, or 'from' after 'to'", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, not a trainer, or not "
                    + "one of your clients", content = @Content)
    })
    @GetMapping("/api/v1/clients/{id}/progress-reports")
    public ResponseEntity<ProgressReportResource> getByClient(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        var query = assembler.toProgressReportQuery(new ClientId(id), authentication.getName(), from, to);
        return ResponseEntity.ok(assembler.toResource(workoutSessionQueryService.handle(query)));
    }
}
