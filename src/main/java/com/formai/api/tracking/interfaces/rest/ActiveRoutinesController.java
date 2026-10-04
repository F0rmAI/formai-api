package com.formai.api.tracking.interfaces.rest;

import com.formai.api.shared.interfaces.rest.ApiTags;
import com.formai.api.tracking.domain.exceptions.ActiveRoutineNotFoundException;
import com.formai.api.tracking.domain.model.commands.ChangeTimeZoneCommand;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutineQuery;
import com.formai.api.tracking.domain.model.queries.GetClientTodayQuery;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.services.ActiveRoutineCommandService;
import com.formai.api.tracking.domain.services.ActiveRoutineQueryService;
import com.formai.api.tracking.interfaces.rest.resources.ActiveRoutineResource;
import com.formai.api.tracking.interfaces.rest.transform.ActiveRoutineAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Tag(name = ApiTags.WORKOUTS, description = ApiTags.WORKOUTS_DESCRIPTION)
@RestController
@RequestMapping("/api/v1/active-routines")
public class ActiveRoutinesController {

    public static final String TIME_ZONE_HEADER = "X-Client-Timezone";

    private final ActiveRoutineQueryService activeRoutineQueryService;
    private final ActiveRoutineCommandService activeRoutineCommandService;
    private final ActiveRoutineAssembler assembler;

    public ActiveRoutinesController(ActiveRoutineQueryService activeRoutineQueryService,
                                    ActiveRoutineCommandService activeRoutineCommandService,
                                    ActiveRoutineAssembler assembler) {
        this.activeRoutineQueryService = activeRoutineQueryService;
        this.activeRoutineCommandService = activeRoutineCommandService;
        this.assembler = assembler;
    }

    @Operation(summary = "Get my active routine",
            description = "Returns the routine the signed-in client is following, with every session of " +
                    "the routine, the latest result of each session and, when one is scheduled for today, " +
                    "today's session order and id. Today is the client's own date: the time zone the app " +
                    "sends in X-Client-Timezone is stored and also drives the daily scheduling. " +
                    "It never creates a session.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active routine",
                    content = @Content(schema = @Schema(implementation = ActiveRoutineResource.class))),
            @ApiResponse(responseCode = "400", description = "Unknown time zone in X-Client-Timezone", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a client", content = @Content),
            @ApiResponse(responseCode = "404", description = "The client has no routine assigned", content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<ActiveRoutineResource> getMine(
            Authentication authentication,
            @Parameter(in = ParameterIn.HEADER, description = "IANA time zone of the device, e.g. America/Lima")
            @RequestHeader(name = TIME_ZONE_HEADER, required = false) String timeZone) {
        var clientId = new ClientId(UUID.fromString(authentication.getName()));
        var now = Instant.now();
        LocalDate today;
        if (timeZone == null || timeZone.isBlank()) {
            today = activeRoutineQueryService.handle(new GetClientTodayQuery(clientId, now));
        } else {
            var zone = ZoneId.of(timeZone.strip());
            activeRoutineCommandService.handle(new ChangeTimeZoneCommand(clientId, zone));
            today = LocalDate.ofInstant(now, zone);
        }
        var plan = activeRoutineQueryService.handle(new GetActiveRoutineQuery(clientId, today))
                .orElseThrow(ActiveRoutineNotFoundException::new);
        return ResponseEntity.ok(assembler.toResource(plan));
    }
}
