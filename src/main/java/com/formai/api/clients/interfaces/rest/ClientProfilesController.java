package com.formai.api.clients.interfaces.rest;

import com.formai.api.clients.domain.exceptions.ClientNotFoundException;
import com.formai.api.clients.domain.model.queries.GetClientProfileQuery;
import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.services.ClientQueryService;
import com.formai.api.clients.interfaces.rest.resources.ClientProfileResource;
import com.formai.api.clients.interfaces.rest.transform.ClientAssembler;
import com.formai.api.shared.interfaces.rest.ApiTags;
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

import java.util.UUID;

// /clients/** belongs to the trainer (web); the signed-in client reads their own record here.
@Tag(name = ApiTags.CLIENTS, description = ApiTags.CLIENTS_DESCRIPTION)
@RestController
@RequestMapping("/api/v1/client-profiles")
public class ClientProfilesController {

    private final ClientQueryService clientQueryService;
    private final ClientAssembler assembler;

    public ClientProfilesController(ClientQueryService clientQueryService, ClientAssembler assembler) {
        this.clientQueryService = clientQueryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Get my profile",
            description = "The name the trainer registered and the email the signed-in client chose on " +
                    "activation, so the mobile app can greet the client by name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The signed-in client's profile",
                    content = @Content(schema = @Schema(implementation = ClientProfileResource.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a client",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "The account has no client record", content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<ClientProfileResource> getMine(Authentication authentication) {
        // A client's id is the id of their iam account, so it is also the JWT subject.
        var clientId = new ClientId(UUID.fromString(authentication.getName()));
        var client = clientQueryService.handle(new GetClientProfileQuery(clientId))
                .orElseThrow(ClientNotFoundException::new);
        return ResponseEntity.ok(assembler.toProfileResource(client));
    }
}
