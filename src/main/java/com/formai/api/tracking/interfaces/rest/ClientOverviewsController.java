package com.formai.api.tracking.interfaces.rest;

import com.formai.api.shared.interfaces.rest.ApiTags;
import com.formai.api.tracking.domain.services.WorkoutSessionQueryService;
import com.formai.api.tracking.interfaces.rest.resources.ClientOverviewPageResource;
import com.formai.api.tracking.interfaces.rest.transform.WorkoutSessionAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = ApiTags.CLIENTS, description = ApiTags.CLIENTS_DESCRIPTION)
@RestController
@RequestMapping("/api/v1/client-overviews")
public class ClientOverviewsController {

    private final WorkoutSessionQueryService workoutSessionQueryService;
    private final WorkoutSessionAssembler assembler;

    public ClientOverviewsController(WorkoutSessionQueryService workoutSessionQueryService,
                                     WorkoutSessionAssembler assembler) {
        this.workoutSessionQueryService = workoutSessionQueryService;
        this.assembler = assembler;
    }

    @Operation(summary = "List my clients with their current routine and last workout",
            description = "Only the trainer's own clients, sorted by name, searched by name and filtered by status "
                    + "(INVITED, ACTIVE or INACTIVE).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of client overviews",
                    content = @Content(schema = @Schema(implementation = ClientOverviewPageResource.class))),
            @ApiResponse(responseCode = "400", description = "An invalid page or size", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<ClientOverviewPageResource> getAll(@RequestParam(required = false) String search,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(defaultValue = "0") @Min(0) int page,
                                                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                             Authentication authentication) {
        var query = assembler.toOverviewsQuery(authentication.getName(), search, status, page, size);
        return ResponseEntity.ok(assembler.toResource(workoutSessionQueryService.handle(query)));
    }
}
