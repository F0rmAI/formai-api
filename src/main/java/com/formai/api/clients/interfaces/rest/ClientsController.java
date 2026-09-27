package com.formai.api.clients.interfaces.rest;

import com.formai.api.clients.domain.exceptions.ClientNotFoundException;
import com.formai.api.clients.domain.model.aggregates.Client;
import com.formai.api.clients.domain.model.commands.DeactivateClientCommand;
import com.formai.api.clients.domain.model.commands.RenewActivationCodeCommand;
import com.formai.api.clients.domain.model.queries.GetClientByIdQuery;
import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.services.ClientCommandService;
import com.formai.api.clients.domain.services.ClientQueryService;
import com.formai.api.clients.interfaces.rest.resources.ActivationCodeResource;
import com.formai.api.clients.interfaces.rest.resources.BodyProfileResource;
import com.formai.api.clients.interfaces.rest.resources.ClientPageResource;
import com.formai.api.clients.interfaces.rest.resources.ClientResource;
import com.formai.api.clients.interfaces.rest.resources.RegisterClientResource;
import com.formai.api.clients.interfaces.rest.resources.RegisteredClientResource;
import com.formai.api.clients.interfaces.rest.resources.UpdateBodyProfileResource;
import com.formai.api.clients.interfaces.rest.resources.UpdateClientResource;
import com.formai.api.clients.interfaces.rest.transform.ClientAssembler;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Tag(name = ApiTags.CLIENTS, description = ApiTags.CLIENTS_DESCRIPTION)
@RestController
@RequestMapping("/api/v1/clients")
public class ClientsController {

    private final ClientCommandService clientCommandService;
    private final ClientQueryService clientQueryService;
    private final ClientAssembler assembler;

    public ClientsController(ClientCommandService clientCommandService,
                             ClientQueryService clientQueryService,
                             ClientAssembler assembler) {
        this.clientCommandService = clientCommandService;
        this.clientQueryService = clientQueryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Register a client",
            description = "Creates the client as INVITED and returns a 72-hour activation code to share by hand.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client registered, with the activation code",
                    content = @Content(schema = @Schema(implementation = RegisteredClientResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing name, or an invalid email", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Already one of your clients, or the email is not available",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<RegisteredClientResource> register(@Valid @RequestBody RegisterClientResource resource,
                                                             Authentication authentication) {
        var registered = clientCommandService.handle(assembler.toCommand(authentication.getName(), resource))
                .orElseThrow(() -> new IllegalStateException("Registering a client should never return empty"));
        return new ResponseEntity<>(assembler.toResource(registered), HttpStatus.CREATED);
    }

    @Operation(summary = "List my clients",
            description = "The trainer's clients sorted by name, optionally searched by name and filtered by status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of clients",
                    content = @Content(schema = @Schema(implementation = ClientPageResource.class))),
            @ApiResponse(responseCode = "400", description = "Unknown status, or an invalid page or size",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<ClientPageResource> getAll(@RequestParam(required = false) String search,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                     Authentication authentication) {
        var query = assembler.toQuery(authentication.getName(), search, status, page, size);
        return ResponseEntity.ok(assembler.toResource(clientQueryService.handle(query)));
    }

    @Operation(summary = "Get one of my clients")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The client",
                    content = @Content(schema = @Schema(implementation = ClientResource.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Not one of your clients", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ClientResource> getById(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(assembler.toResource(findOwnClient(id, authentication)));
    }

    @Operation(summary = "Rename a client")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client updated",
                    content = @Content(schema = @Schema(implementation = ClientResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing or too long name", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Not one of your clients", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<ClientResource> update(@PathVariable UUID id,
                                                 @Valid @RequestBody UpdateClientResource resource,
                                                 Authentication authentication) {
        var client = clientCommandService.handle(assembler.toCommand(id, authentication.getName(), resource));
        return ResponseEntity.ok(assembler.toResource(saved(client)));
    }

    @Operation(summary = "Deactivate a client",
            description = "The client can no longer sign in and their current routine is closed; their history is kept.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client deactivated",
                    content = @Content(schema = @Schema(implementation = ClientResource.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Not one of your clients", content = @Content)
    })
    @PostMapping("/{id}/deactivations")
    public ResponseEntity<ClientResource> deactivate(@PathVariable UUID id, Authentication authentication) {
        var client = clientCommandService.handle(new DeactivateClientCommand(new ClientId(id), authentication.getName()));
        return new ResponseEntity<>(assembler.toResource(saved(client)), HttpStatus.CREATED);
    }

    @Operation(summary = "Get a new activation code",
            description = "For a client who has not activated their account yet. The previous code stops working.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "New activation code",
                    content = @Content(schema = @Schema(implementation = ActivationCodeResource.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Not one of your clients", content = @Content),
            @ApiResponse(responseCode = "409", description = "The client already activated their account",
                    content = @Content)
    })
    @PostMapping("/{id}/activation-codes")
    public ResponseEntity<ActivationCodeResource> renewActivationCode(@PathVariable UUID id,
                                                                      Authentication authentication) {
        var ticket = clientCommandService.handle(new RenewActivationCodeCommand(new ClientId(id), authentication.getName()))
                .orElseThrow(() -> new IllegalStateException("Renewing a code should never return empty"));
        return new ResponseEntity<>(assembler.toResource(ticket), HttpStatus.CREATED);
    }

    @Operation(summary = "Get a client's body profile",
            description = "Goal, height, current weight, restrictions and every recorded weight with its date.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The body profile",
                    content = @Content(schema = @Schema(implementation = BodyProfileResource.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Not one of your clients, or no body profile yet",
                    content = @Content)
    })
    @GetMapping("/{id}/body-profile")
    public ResponseEntity<BodyProfileResource> getBodyProfile(@PathVariable UUID id, Authentication authentication) {
        var profile = findOwnClient(id, authentication).getBodyProfile()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "This client has no body profile yet"));
        return ResponseEntity.ok(assembler.toResource(profile));
    }

    @Operation(summary = "Save a client's body profile",
            description = "Creates or updates it. A new weight is added to the history with today's date.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Body profile saved",
                    content = @Content(schema = @Schema(implementation = BodyProfileResource.class))),
            @ApiResponse(responseCode = "400", description = "A missing field", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie, or not a trainer",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Not one of your clients", content = @Content),
            @ApiResponse(responseCode = "422", description = "Weight not above 0 kg or height outside 100-250 cm",
                    content = @Content)
    })
    @PutMapping("/{id}/body-profile")
    public ResponseEntity<BodyProfileResource> updateBodyProfile(@PathVariable UUID id,
                                                                 @Valid @RequestBody UpdateBodyProfileResource resource,
                                                                 Authentication authentication) {
        var client = saved(clientCommandService.handle(assembler.toCommand(id, authentication.getName(), resource)));
        var profile = client.getBodyProfile()
                .orElseThrow(() -> new IllegalStateException("A saved body profile should never be empty"));
        return ResponseEntity.ok(assembler.toResource(profile));
    }

    private Client findOwnClient(UUID id, Authentication authentication) {
        return clientQueryService.handle(new GetClientByIdQuery(new ClientId(id), authentication.getName()))
                .orElseThrow(ClientNotFoundException::new);
    }

    private Client saved(Optional<Client> client) {
        return client.orElseThrow(() -> new IllegalStateException("A client command should never return empty"));
    }
}
