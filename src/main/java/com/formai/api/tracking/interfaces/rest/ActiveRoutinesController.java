package com.formai.api.tracking.interfaces.rest;

import com.formai.api.shared.interfaces.rest.ApiTags;
import com.formai.api.tracking.domain.exceptions.ActiveRoutineNotFoundException;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutineQuery;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.services.ActiveRoutineQueryService;
import com.formai.api.tracking.interfaces.rest.resources.ActiveRoutineResource;
import com.formai.api.tracking.interfaces.rest.transform.ActiveRoutineAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = ApiTags.WORKOUTS, description = ApiTags.WORKOUTS_DESCRIPTION)
@RestController
@RequestMapping("/api/v1/active-routines")
public class ActiveRoutinesController {

    private final ActiveRoutineQueryService activeRoutineQueryService;
    private final ActiveRoutineAssembler assembler;

    public ActiveRoutinesController(ActiveRoutineQueryService activeRoutineQueryService,
                                    ActiveRoutineAssembler assembler) {
        this.activeRoutineQueryService = activeRoutineQueryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Get my active routine",
            description = "Returns the routine the signed-in client is following, with every session of " +
                    "the routine and, when one is scheduled for today, today's session order and id. " +
                    "It never creates a session.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active routine",
                    content = @Content(schema = @Schema(implementation = ActiveRoutineResource.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a client", content = @Content),
            @ApiResponse(responseCode = "404", description = "The client has no routine assigned", content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<ActiveRoutineResource> getMine(Authentication authentication) {
        var clientId = new ClientId(UUID.fromString(authentication.getName()));
        var plan = activeRoutineQueryService.handle(new GetActiveRoutineQuery(clientId, LocalDate.now()))
                .orElseThrow(ActiveRoutineNotFoundException::new);
        return ResponseEntity.ok(assembler.toResource(plan));
    }
}
